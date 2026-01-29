package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseDeletionService {
    
    private final IDriverProductBalanceService driverProductBalanceService;
    private final PurchaseTransactionService transactionService;
    
    public void deletePurchase(@NonNull Purchase purchase) {
        removeProductFromDriverBalance(purchase);
        transactionService.deletePurchaseTransaction(purchase);
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
}
