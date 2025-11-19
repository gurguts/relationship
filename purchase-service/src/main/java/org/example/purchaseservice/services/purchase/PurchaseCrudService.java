package org.example.purchaseservice.services.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.AccountClient;
import org.example.purchaseservice.clients.AccountTransactionClient;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.clients.SourceClient;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.clients.UserClient;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.exceptions.PurchaseNotFoundException;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.account.AccountDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.purchaseservice.models.transaction.TransactionType;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.services.impl.IPurchaseCrudService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseCrudService implements IPurchaseCrudService {
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final TransactionApiClient transactionApiClient;
    private final AccountTransactionClient accountTransactionClient;
    private final AccountClient accountClient;
    private final SourceClient sourceClient;
    private final UserClient userCLient;
    private final org.example.purchaseservice.services.balance.DriverProductBalanceService driverProductBalanceService;
    private final WarehouseReceiptRepository warehouseReceiptRepository;

    @Override
    @Transactional
    public Purchase createPurchase(Purchase purchase) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        purchase.setUser(userId);

        purchase.calculateAndSetUnitPrice();
        purchase.calculateAndSetPricesInUah(); // Convert prices to UAH

        Long transactionId = null;
        if (purchase.getPaymentMethod().equals(PaymentMethod.CASH)) {

            transactionId = createAccountTransactionForPurchase(userId, purchase);
        }

        purchase.setTransaction(transactionId);

        clientApiClient.setUrgentlyFalseAndRoute(purchase.getClient());

        Purchase savedPurchase = purchaseRepository.save(purchase);

        // Update driver product balance
        if (savedPurchase.getTotalPriceUah() != null && savedPurchase.getQuantity() != null) {
            driverProductBalanceService.addProduct(
                    userId,
                    savedPurchase.getProduct(),
                    savedPurchase.getQuantity(),
                    savedPurchase.getTotalPriceUah()  // Use total price, not unit price!
            );
        }

        return savedPurchase;
    }

    @Override
    @Transactional
    public Purchase updatePurchase(Long id, Purchase updatedPurchase) {
        Purchase existingPurchase = findPurchaseById(id);

        if (isPurchaseReceived(existingPurchase)) {
            throw new PurchaseException("PURCHASE_RECEIVED", 
                    "Неможливо редагувати закупку, оскільки товар вже прийнято кладовщиком.");
        }

        String fullName = getFullName();

        String sourceName = sourceClient.getSourceName(existingPurchase.getSource()).getName();

        boolean canEditData = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> "purchase:edit_strangers".equals(auth.getAuthority()));

        if (!(fullName.equals(sourceName) || canEditData)) {
            throw new PurchaseException("ONLY_OWNER", "You cannot change someone else's purchase.");
        }

        // Save old values for balance update
        java.math.BigDecimal oldQuantity = existingPurchase.getQuantity();
        java.math.BigDecimal oldTotalPriceUah = existingPurchase.getTotalPriceUah();
        Long productId = existingPurchase.getProduct();

        if (updatedPurchase.getProduct() != null && !Objects.equals(updatedPurchase.getProduct(),
                existingPurchase.getProduct())) {
            existingPurchase.setProduct(updatedPurchase.getProduct());
            productId = updatedPurchase.getProduct();
        }

        boolean needsBalanceUpdate = false;

        if (updatedPurchase.getQuantity() != null &&
                (existingPurchase.getQuantity() == null || updatedPurchase.getQuantity().compareTo(
                        existingPurchase.getQuantity()) != 0)) {
            existingPurchase.setQuantity(updatedPurchase.getQuantity());
            existingPurchase.calculateAndSetUnitPrice();
            needsBalanceUpdate = true;
        }

        if (updatedPurchase.getTotalPrice() != null &&
                (existingPurchase.getTotalPrice() == null || updatedPurchase.getTotalPrice().compareTo(
                        existingPurchase.getTotalPrice()) != 0)) {
            existingPurchase.setTotalPrice(updatedPurchase.getTotalPrice());
            existingPurchase.calculateAndSetUnitPrice();
            needsBalanceUpdate = true;

            if (existingPurchase.getTransaction() != null) {
                transactionApiClient.updateTransactionAmount(
                        existingPurchase.getTransaction(), updatedPurchase.getTotalPrice());
            }
        }

        if (updatedPurchase.getCreatedAt() != null &&
                (existingPurchase.getCreatedAt() == null ||
                        !updatedPurchase.getCreatedAt().toLocalDate().equals(
                                existingPurchase.getCreatedAt().toLocalDate()))) {
            existingPurchase.setCreatedAt(updatedPurchase.getCreatedAt());
        }

        if (updatedPurchase.getSource() != null && !Objects.equals(updatedPurchase.getSource(),
                existingPurchase.getSource())) {
            boolean canEditSource = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(auth -> "purchase:edit_source".equals(auth.getAuthority()));

            if (!canEditSource) {
                throw new PurchaseException("ONLY_ADMIN", "Only users with ADMIN role can update sourceId");
            }
            existingPurchase.setSource(updatedPurchase.getSource());
        }

        existingPurchase.setExchangeRate(updatedPurchase.getExchangeRate());

        if (updatedPurchase.getComment() != null) {
            existingPurchase.setComment(updatedPurchase.getComment());
        }

        // Recalculate prices in UAH
        existingPurchase.calculateAndSetPricesInUah();

        Purchase savedPurchase = purchaseRepository.save(existingPurchase);

        // Update driver balance if quantity or price changed
        if (needsBalanceUpdate && savedPurchase.getTotalPriceUah() != null) {
            driverProductBalanceService.updateFromPurchaseChange(
                    savedPurchase.getUser(),
                    productId,
                    oldQuantity,
                    oldTotalPriceUah,  // Use total price!
                    savedPurchase.getQuantity(),
                    savedPurchase.getTotalPriceUah()  // Use total price!
            );
        }

        return savedPurchase;
    }

    private String getFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();

        return userCLient.getUserFullNameFromLogin(login);
    }

    private Long createAccountTransactionForPurchase(Long userId, Purchase purchase) {

        List<AccountDTO> userAccounts = accountClient.getAccountsByUserId(userId);
        
        if (userAccounts == null || userAccounts.isEmpty()) {
            throw new PurchaseException("NO_ACCOUNTS", 
                    String.format("У користувача з ID %d немає рахунків. Створіть рахунок перед створенням закупки.", userId));
        }

        String transactionCurrency = purchase.getCurrency() != null ? purchase.getCurrency() : "UAH";

        AccountDTO account = findAccountWithCurrency(userAccounts, transactionCurrency);
        
        if (account == null) {
            throw new PurchaseException("NO_ACCOUNT_WITH_CURRENCY", 
                    String.format("У користувача з ID %d немає рахунку з валютою %s. Створіть рахунок з цією валютою.", 
                            userId, transactionCurrency));
        }

        TransactionCreateRequestDTO transactionRequest = new TransactionCreateRequestDTO();
        transactionRequest.setType(TransactionType.CLIENT_PAYMENT);
        transactionRequest.setFromAccountId(account.getId());
        transactionRequest.setAmount(purchase.getTotalPrice());
        transactionRequest.setCurrency(transactionCurrency);
        transactionRequest.setClientId(purchase.getClient());

        transactionRequest.setDescription(purchase.getComment());

        var transactionDTO = accountTransactionClient.createTransaction(transactionRequest);
        return transactionDTO.getId();
    }

    private AccountDTO findAccountWithCurrency(List<AccountDTO> accounts, String currency) {
        if (currency == null || currency.isEmpty() || "UAH".equals(currency)) {

            return accounts.stream()
                    .filter(acc -> acc.getCurrencies() != null && 
                            (acc.getCurrencies().contains("UAH") || acc.getCurrencies().isEmpty()))
                    .findFirst()
                    .orElse(null);
        }

        return accounts.stream()
                .filter(acc -> acc.getCurrencies() != null && acc.getCurrencies().contains(currency))
                .findFirst()
                .orElse(null);
    }

    private boolean isPurchaseReceived(Purchase purchase) {
        if (purchase == null || purchase.getUser() == null || purchase.getProduct() == null 
                || purchase.getCreatedAt() == null) {
            return false;
        }

        Specification<WarehouseReceipt> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("userId"), purchase.getUser()),
                cb.equal(root.get("productId"), purchase.getProduct()),
                cb.greaterThanOrEqualTo(root.get("createdAt"), purchase.getCreatedAt())
        );
        
        List<WarehouseReceipt> receipts = warehouseReceiptRepository.findAll(spec);

        return !receipts.isEmpty();
    }

    @Override
    public Purchase findPurchaseById(Long id) {
        if (id == null) {
            throw new PurchaseException("ID cannot be null");
        }
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new PurchaseNotFoundException(String.format("Purchase with ID %d not found", id)));
    }

    public void enrichPurchaseDTOWithReceivedStatus(org.example.purchaseservice.models.dto.purchase.PurchaseDTO dto, Purchase purchase) {
        if (dto != null && purchase != null) {
            dto.setIsReceived(isPurchaseReceived(purchase));
        }
    }

    @Override
    @Transactional
    public void deletePurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() ->
                new PurchaseNotFoundException(String.format("Purchase with ID %d not found", id)));
        
        // Remove from driver balance before deleting purchase
        if (purchase.getQuantity() != null && purchase.getQuantity().compareTo(java.math.BigDecimal.ZERO) > 0 
                && purchase.getTotalPriceUah() != null) {
            try {
                driverProductBalanceService.removeProduct(
                        purchase.getUser(),
                        purchase.getProduct(),
                        purchase.getQuantity(),
                        purchase.getTotalPriceUah()  // Pass the specific total price of this purchase!
                );
            } catch (Exception e) {
                // Log error but don't interrupt deletion
                // (balance might have been already removed via warehouse entry)
                log.warn("Could not remove product from driver balance during purchase deletion: {}", e.getMessage());
            }
        }
        
        transactionApiClient.deleteTransaction(purchase.getTransaction());
        purchaseRepository.deleteById(id);
    }
}
