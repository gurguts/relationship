package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.services.exchange.IExchangeRateService;
import org.example.purchaseservice.services.impl.IPurchasePriceCalculationService;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseUpdateService {
    
    private static final String DEFAULT_PURCHASE_CURRENCY = "EUR";
    private static final String AUTHORITY_EDIT_SOURCE = "purchase:edit_source";
    
    private final IPurchasePriceCalculationService purchaseService;
    private final IExchangeRateService exchangeRateService;
    private final PurchaseTransactionService transactionService;
    
    public record UpdateResult(Long productId, boolean needsBalanceUpdate) {}
    
    public UpdateResult updatePurchase(@NonNull Purchase existingPurchase, @NonNull Purchase updatedPurchase) {
        Long productId = updatePurchaseProduct(existingPurchase, updatedPurchase);
        
        boolean needsBalanceUpdate = updatePurchaseQuantity(existingPurchase, updatedPurchase);
        needsBalanceUpdate = updatePurchaseTotalPrice(existingPurchase, updatedPurchase) || needsBalanceUpdate;
        
        updatePurchaseCreatedAt(existingPurchase, updatedPurchase);
        updatePurchaseSource(existingPurchase, updatedPurchase);
        updatePurchaseExchangeRateAndComment(existingPurchase, updatedPurchase);
        updatePurchaseCurrencyAndPrices(existingPurchase);
        
        return new UpdateResult(productId, needsBalanceUpdate);
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
                transactionService.updateTransactionAmount(existingPurchase.getTransaction(), updatedPurchase.getTotalPrice());
            }
            return true;
        }
        return false;
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
}
