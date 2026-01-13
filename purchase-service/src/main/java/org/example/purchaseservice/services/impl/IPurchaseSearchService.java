package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.PurchasePageDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IPurchaseSearchService {
    PageResponse<PurchasePageDTO> searchPurchase(String query, @NonNull Pageable pageable, Map<String, List<String>> filterParams);

    List<Purchase> getPurchasesByClientId(@NonNull Long clientId);

    List<Purchase> searchForWarehouse(Map<String, List<String>> filters);

    PurchaseReportDTO generateReport(String query, Map<String, List<String>> filterParams);
}
