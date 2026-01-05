package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.PurchaseDTO;

public interface IPurchaseCrudService {
    Purchase createPurchase(Purchase purchase);

    Purchase updatePurchase(Long id, Purchase updatedPurchase);

    Purchase findPurchaseById(Long id);

    void deletePurchase(Long id);

    void enrichPurchaseDTOWithReceivedStatus(PurchaseDTO dto, Purchase purchase);
}
