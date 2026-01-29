package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseNotFoundException;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.example.purchaseservice.services.impl.IPurchaseCrudService;
import org.example.purchaseservice.services.impl.IPurchasePriceCalculationService;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.purchaseservice.services.exchange.IExchangeRateService;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseCrudService implements IPurchaseCrudService {
    
    private static final String DEFAULT_PURCHASE_CURRENCY = "EUR";
    
    private final PurchaseRepository purchaseRepository;
    private final IPurchasePriceCalculationService purchaseService;
    private final IExchangeRateService exchangeRateService;
    private final IDriverProductBalanceService driverProductBalanceService;
    private final PurchaseValidator validator;
    private final PurchaseUpdateService updateService;
    private final PurchaseTransactionService transactionService;
    private final PurchaseDeletionService deletionService;

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
            transactionId = transactionService.createAccountTransactionForPurchase(purchase.getUser(), purchase);
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

        validator.validatePurchaseUpdatePermissions(existingPurchase);
        
        BigDecimal oldQuantity = existingPurchase.getQuantity();
        BigDecimal oldTotalPriceEur = existingPurchase.getTotalPriceEur();
        
        PurchaseUpdateService.UpdateResult updateResult = updateService.updatePurchase(existingPurchase, updatedPurchase);

        Purchase savedPurchase = purchaseRepository.save(existingPurchase);
        log.info("Purchase updated: id={}, userId={}, productId={}", 
                savedPurchase.getId(), savedPurchase.getUser(), savedPurchase.getProduct());

        if (updateResult.needsBalanceUpdate() && savedPurchase.getTotalPriceEur() != null) {
            driverProductBalanceService.updateFromPurchaseChange(
                    savedPurchase.getUser(),
                    updateResult.productId(),
                    oldQuantity,
                    oldTotalPriceEur,
                    savedPurchase.getQuantity(),
                    savedPurchase.getTotalPriceEur()
            );
        }

        return savedPurchase;
    }

    private String getPurchaseCurrencyOrDefault(String currency) {
        return currency != null ? currency : DEFAULT_PURCHASE_CURRENCY;
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
        dto.setIsReceived(validator.isPurchaseReceived(purchase));
    }

    @Override
    @Transactional
    public void deletePurchase(@NonNull Long id) {
        log.info("Deleting purchase: id={}", id);
        
        Purchase purchase = findPurchaseById(id);

        deletionService.deletePurchase(purchase);
        
        purchaseRepository.deleteById(id);
        log.info("Purchase deleted: id={}", id);
    }
}
