package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.PurchaseDTO;

public interface IPurchaseCrudService {
    Purchase createPurchase(@NonNull Purchase purchase);

    Purchase updatePurchase(@NonNull Long id, @NonNull Purchase updatedPurchase);

    Purchase findPurchaseById(@NonNull Long id);

    void deletePurchase(@NonNull Long id);

    void enrichPurchaseDTOWithReceivedStatus(@NonNull PurchaseDTO dto, @NonNull Purchase purchase);
}
