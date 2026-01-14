package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.AccountClient;
import org.example.purchaseservice.clients.AccountTransactionClient;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.services.impl.ISourceService;
import org.example.purchaseservice.services.impl.IUserService;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.exceptions.PurchaseNotFoundException;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.account.AccountDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionDTO;
import org.example.purchaseservice.models.transaction.TransactionType;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.services.impl.IPurchaseCrudService;
import org.example.purchaseservice.services.impl.IPurchasePriceCalculationService;
import org.springframework.data.jpa.domain.Specification;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.purchaseservice.services.exchange.IExchangeRateService;
import feign.FeignException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseCrudService implements IPurchaseCrudService {
    
    private static final String DEFAULT_PURCHASE_CURRENCY = "EUR";
    private static final String DEFAULT_TRANSACTION_CURRENCY = "UAH";
    private static final String UAH_CURRENCY = "UAH";
    private static final String AUTHORITY_EDIT_STRANGERS = "purchase:edit_strangers";
    private static final String AUTHORITY_EDIT_SOURCE = "purchase:edit_source";
    
    private final PurchaseRepository purchaseRepository;
    private final TransactionApiClient transactionApiClient;
    private final AccountTransactionClient accountTransactionClient;
    private final AccountClient accountClient;
    private final ISourceService sourceService;
    private final IUserService userService;
    private final IDriverProductBalanceService driverProductBalanceService;
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final IExchangeRateService exchangeRateService;
    private final IPurchasePriceCalculationService purchaseService;

    @Override
    @Transactional
    public Purchase createPurchase(@NonNull Purchase purchase) {
        log.info("Creating new purchase: userId={}, productId={}, quantity={}", 
                purchase.getUser(), purchase.getProduct(), purchase.getQuantity());
        
        Long executedUserId = SecurityUtils.getCurrentUserId();
        purchase.setExecutedUser(executedUserId);

        purchaseService.calculateAndSetUnitPrice(purchase);
        
        String purchaseCurrency = getPurchaseCurrencyOrDefault(purchase.getCurrency());
        BigDecimal exchangeRateToEur = exchangeRateService.getExchangeRateToEur(purchaseCurrency);
        purchase.setExchangeRateToEur(exchangeRateToEur);
        purchaseService.calculateAndSetPricesInEur(purchase, exchangeRateToEur);

        Long transactionId = null;
        if (PaymentMethod.CASH.equals(purchase.getPaymentMethod())) {
            transactionId = createAccountTransactionForPurchase(purchase.getUser(), purchase);
        }

        purchase.setTransaction(transactionId);

        Purchase savedPurchase = purchaseRepository.save(purchase);
        log.info("Purchase created: id={}, userId={}, productId={}", 
                savedPurchase.getId(), savedPurchase.getUser(), savedPurchase.getProduct());

        if (savedPurchase.getTotalPriceEur() != null && savedPurchase.getQuantity() != null) {
            driverProductBalanceService.addProduct(
                    savedPurchase.getUser(),
                    savedPurchase.getProduct(),
                    savedPurchase.getQuantity(),
                    savedPurchase.getTotalPriceEur()
            );
        }

        return savedPurchase;
    }

    @Override
    @Transactional
    public Purchase updatePurchase(@NonNull Long id, @NonNull Purchase updatedPurchase) {
        log.info("Updating purchase: id={}", id);
        
        Purchase existingPurchase = findPurchaseById(id);

        validatePurchaseUpdatePermissions(existingPurchase);
        
        BigDecimal oldQuantity = existingPurchase.getQuantity();
        BigDecimal oldTotalPriceEur = existingPurchase.getTotalPriceEur();
        Long productId = updatePurchaseProduct(existingPurchase, updatedPurchase);
        
        boolean needsBalanceUpdate = updatePurchaseQuantity(existingPurchase, updatedPurchase);
        needsBalanceUpdate = updatePurchaseTotalPrice(existingPurchase, updatedPurchase) || needsBalanceUpdate;
        
        updatePurchaseCreatedAt(existingPurchase, updatedPurchase);
        updatePurchaseSource(existingPurchase, updatedPurchase);
        updatePurchaseExchangeRateAndComment(existingPurchase, updatedPurchase);
        
        updatePurchaseCurrencyAndPrices(existingPurchase);

        Purchase savedPurchase = purchaseRepository.save(existingPurchase);
        log.info("Purchase updated: id={}, userId={}, productId={}", 
                savedPurchase.getId(), savedPurchase.getUser(), savedPurchase.getProduct());

        if (needsBalanceUpdate && savedPurchase.getTotalPriceEur() != null) {
            driverProductBalanceService.updateFromPurchaseChange(
                    savedPurchase.getUser(),
                    productId,
                    oldQuantity,
                    oldTotalPriceEur,
                    savedPurchase.getQuantity(),
                    savedPurchase.getTotalPriceEur()
            );
        }

        return savedPurchase;
    }
    
    private void validatePurchaseUpdatePermissions(@NonNull Purchase existingPurchase) {
        if (isPurchaseReceived(existingPurchase)) {
            throw new PurchaseException("PURCHASE_RECEIVED", 
                    "Неможливо редагувати закупку, оскільки товар вже прийнято кладовщиком.");
        }

        String fullName = getFullName();
        boolean canEditData = SecurityUtils.hasAuthority(AUTHORITY_EDIT_STRANGERS);

        if (existingPurchase.getSource() != null) {
            SourceDTO sourceResponse = sourceService.getSourceName(existingPurchase.getSource());
            String sourceName = sourceResponse.getName();
            boolean isOwner = fullName != null && fullName.equals(sourceName);
            if (!isOwner && !canEditData) {
                throw new PurchaseException("ONLY_OWNER", "You cannot change someone else's purchase.");
            }
        }
    }
    
    private Long updatePurchaseProduct(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        Long productId = existingPurchase.getProduct();
        if (updatedPurchase.getProduct() != null && !Objects.equals(updatedPurchase.getProduct(),
                existingPurchase.getProduct())) {
            existingPurchase.setProduct(updatedPurchase.getProduct());
            productId = updatedPurchase.getProduct();
        }
        return productId;
    }
    
    private boolean updatePurchaseQuantity(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        if (updatedPurchase.getQuantity() != null &&
                (existingPurchase.getQuantity() == null || updatedPurchase.getQuantity().compareTo(
                        existingPurchase.getQuantity()) != 0)) {
            existingPurchase.setQuantity(updatedPurchase.getQuantity());
            purchaseService.calculateAndSetUnitPrice(existingPurchase);
            return true;
        }
        return false;
    }
    
    private boolean updatePurchaseTotalPrice(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        if (updatedPurchase.getTotalPrice() != null &&
                (existingPurchase.getTotalPrice() == null || updatedPurchase.getTotalPrice().compareTo(
                        existingPurchase.getTotalPrice()) != 0)) {
            existingPurchase.setTotalPrice(updatedPurchase.getTotalPrice());
            purchaseService.calculateAndSetUnitPrice(existingPurchase);

            if (existingPurchase.getTransaction() != null) {
                updateTransactionAmount(existingPurchase.getTransaction(), updatedPurchase.getTotalPrice());
            }
            return true;
        }
        return false;
    }
    
    private void updateTransactionAmount(@NonNull Long transactionId, @NonNull BigDecimal newAmount) {
        try {
            transactionApiClient.updateTransactionAmount(transactionId, newAmount);
        } catch (FeignException e) {
            log.error("Feign error updating transaction amount: transactionId={}, status={}, error={}", 
                    transactionId, e.status(), e.getMessage(), e);
            throw new PurchaseException("TRANSACTION_UPDATE_FAILED", 
                    String.format("Failed to update transaction amount: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error updating transaction amount: transactionId={}", transactionId, e);
            throw new PurchaseException("TRANSACTION_UPDATE_FAILED", 
                    String.format("Failed to update transaction amount: %s", e.getMessage()), e);
        }
    }
    
    private void updatePurchaseCreatedAt(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        if (updatedPurchase.getCreatedAt() != null &&
                (existingPurchase.getCreatedAt() == null ||
                        !updatedPurchase.getCreatedAt().toLocalDate().equals(
                                existingPurchase.getCreatedAt().toLocalDate()))) {
            existingPurchase.setCreatedAt(updatedPurchase.getCreatedAt());
        }
    }
    
    private void updatePurchaseSource(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        if (updatedPurchase.getSource() != null && !Objects.equals(updatedPurchase.getSource(),
                existingPurchase.getSource())) {
            boolean canEditSource = SecurityUtils.hasAuthority(AUTHORITY_EDIT_SOURCE);

            if (!canEditSource) {
                throw new PurchaseException("ONLY_ADMIN", "Only users with ADMIN role can update sourceId");
            }
            existingPurchase.setSource(updatedPurchase.getSource());
        }
    }
    
    private void updatePurchaseExchangeRateAndComment(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        existingPurchase.setExchangeRate(updatedPurchase.getExchangeRate());

        if (updatedPurchase.getComment() != null) {
            existingPurchase.setComment(updatedPurchase.getComment());
        }
    }
    
    private void updatePurchaseCurrencyAndPrices(@NonNull Purchase existingPurchase) {
        String purchaseCurrency = getPurchaseCurrencyOrDefault(existingPurchase.getCurrency());
        BigDecimal exchangeRateToEur = exchangeRateService.getExchangeRateToEur(purchaseCurrency);
        existingPurchase.setExchangeRateToEur(exchangeRateToEur);
        purchaseService.calculateAndSetPricesInEur(existingPurchase, exchangeRateToEur);
    }
    
    private String getPurchaseCurrencyOrDefault(String currency) {
        return currency != null ? currency : DEFAULT_PURCHASE_CURRENCY;
    }

    private String getFullName() {
        String login = SecurityUtils.getCurrentUserLogin();
        if (login == null || login.trim().isEmpty()) {
            return null;
        }
        
        return userService.getUserFullNameFromLogin(login);
    }

    private Long createAccountTransactionForPurchase(@NonNull Long userId, @NonNull Purchase purchase) {
        List<AccountDTO> userAccounts = getUserAccounts(userId);
        validateUserHasAccounts(userId, userAccounts);
        
        String transactionCurrency = getTransactionCurrencyOrDefault(purchase.getCurrency());
        AccountDTO account = findAccountForTransaction(userId, userAccounts, transactionCurrency);
        
        TransactionCreateRequestDTO transactionRequest = buildTransactionRequest(account, purchase, transactionCurrency);
        return executeTransactionCreation(transactionRequest);
    }
    
    private List<AccountDTO> getUserAccounts(@NonNull Long userId) {
        try {
            List<AccountDTO> userAccounts = accountClient.getAccountsByUserId(userId).getBody();
            return userAccounts != null ? userAccounts : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    private void validateUserHasAccounts(@NonNull Long userId, @NonNull List<AccountDTO> userAccounts) {
        if (userAccounts.isEmpty()) {
            throw new PurchaseException("NO_ACCOUNTS", 
                    String.format("У користувача з ID %d немає рахунків. Створіть рахунок перед створенням закупки.", userId));
        }
    }
    
    private AccountDTO findAccountForTransaction(@NonNull Long userId, @NonNull List<AccountDTO> userAccounts, 
                                                   @NonNull String transactionCurrency) {
        AccountDTO account = findAccountWithCurrency(userAccounts, transactionCurrency);
        
        if (account == null) {
            throw new PurchaseException("NO_ACCOUNT_WITH_CURRENCY", 
                    String.format("У користувача з ID %d немає рахунку з валютою %s. Створіть рахунок з цією валютою.", 
                            userId, transactionCurrency));
        }
        
        return account;
    }
    
    private TransactionCreateRequestDTO buildTransactionRequest(@NonNull AccountDTO account, 
                                                                 @NonNull Purchase purchase, 
                                                                 @NonNull String transactionCurrency) {
        TransactionCreateRequestDTO transactionRequest = new TransactionCreateRequestDTO();
        transactionRequest.setType(TransactionType.CLIENT_PAYMENT);
        transactionRequest.setFromAccountId(account.getId());
        transactionRequest.setAmount(purchase.getTotalPrice());
        transactionRequest.setCurrency(transactionCurrency);
        transactionRequest.setClientId(purchase.getClient());
        transactionRequest.setDescription(purchase.getComment());
        return transactionRequest;
    }
    
    private Long executeTransactionCreation(@NonNull TransactionCreateRequestDTO transactionRequest) {
        try {
            TransactionDTO transactionDTO = accountTransactionClient.createTransaction(transactionRequest).getBody();
            if (transactionDTO == null || transactionDTO.getId() == null) {
                throw new PurchaseException("TRANSACTION_CREATION_FAILED", 
                        "Failed to create transaction: response body or transaction ID is null");
            }
            return transactionDTO.getId();
        } catch (FeignException e) {
            log.error("Feign error creating transaction: accountId={}, status={}, error={}", 
                    transactionRequest.getFromAccountId(), e.status(), e.getMessage());
            throw new PurchaseException("TRANSACTION_CREATION_FAILED", 
                    String.format("Failed to create transaction: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error creating transaction: accountId={}", 
                    transactionRequest.getFromAccountId(), e);
            throw new PurchaseException("TRANSACTION_CREATION_FAILED", 
                    String.format("Failed to create transaction: %s", e.getMessage()), e);
        }
    }

    private String getTransactionCurrencyOrDefault(String currency) {
        return currency != null ? currency : DEFAULT_TRANSACTION_CURRENCY;
    }
    
    private AccountDTO findAccountWithCurrency(@NonNull List<AccountDTO> accounts, String currency) {
        if (accounts.isEmpty()) {
            return null;
        }
        
        if (currency == null || currency.isEmpty() || UAH_CURRENCY.equals(currency)) {
            return accounts.stream()
                    .filter(acc -> acc != null && acc.getCurrencies() != null && 
                            (acc.getCurrencies().contains(UAH_CURRENCY) || acc.getCurrencies().isEmpty()))
                    .findFirst()
                    .orElse(null);
        }

        return accounts.stream()
                .filter(acc -> acc != null && acc.getCurrencies() != null && acc.getCurrencies().contains(currency))
                .findFirst()
                .orElse(null);
    }

    protected boolean isPurchaseReceived(@NonNull Purchase purchase) {
        if (purchase.getUser() == null || purchase.getProduct() == null || purchase.getCreatedAt() == null) {
            return false;
        }

        Specification<WarehouseReceipt> spec = (root, _, cb) -> cb.and(
                cb.equal(root.get("userId"), purchase.getUser()),
                cb.equal(root.get("productId"), purchase.getProduct()),
                cb.greaterThanOrEqualTo(root.get("createdAt"), purchase.getCreatedAt())
        );
        
        List<WarehouseReceipt> receipts = warehouseReceiptRepository.findAll(spec);

        return !receipts.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public Purchase findPurchaseById(@NonNull Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new PurchaseNotFoundException(String.format("Purchase with ID %d not found", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichPurchaseDTOWithReceivedStatus(@NonNull org.example.purchaseservice.models.dto.purchase.PurchaseDTO dto, @NonNull Purchase purchase) {
        dto.setIsReceived(isPurchaseReceived(purchase));
    }

    @Override
    @Transactional
    public void deletePurchase(@NonNull Long id) {
        log.info("Deleting purchase: id={}", id);
        
        Purchase purchase = findPurchaseById(id);

        removeProductFromDriverBalance(purchase);
        deletePurchaseTransaction(purchase);
        
        purchaseRepository.deleteById(id);
        log.info("Purchase deleted: id={}", id);
    }
    
    private void removeProductFromDriverBalance(@NonNull Purchase purchase) {
        if (purchase.getQuantity() != null && purchase.getQuantity().compareTo(BigDecimal.ZERO) > 0 
                && purchase.getTotalPriceEur() != null) {
            try {
                driverProductBalanceService.removeProduct(
                        purchase.getUser(),
                        purchase.getProduct(),
                        purchase.getQuantity(),
                        purchase.getTotalPriceEur()
                );
            } catch (RuntimeException _) {
            }
        }
    }
    
    private void deletePurchaseTransaction(@NonNull Purchase purchase) {
        if (purchase.getTransaction() != null) {
            try {
                transactionApiClient.deleteTransaction(purchase.getTransaction());
            } catch (RuntimeException _) {
            }
        }
    }
}
