package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.PurchasePageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IPurchaseSearchService {
    PageResponse<PurchasePageDTO> searchPurchase(String query, Pageable pageable, Map<String, List<String>> filterParams);

    List<Purchase> getPurchasesByClientId(Long clientId);

    List<Purchase> searchForWarehouse(Map<String, List<String>> filters);

}
