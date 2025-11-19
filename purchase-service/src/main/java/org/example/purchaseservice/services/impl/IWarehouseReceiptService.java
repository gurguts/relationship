package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IWarehouseReceiptService {
    PageResponse<WarehouseReceiptDTO> getWarehouseReceipts(int page, int size, String sort, String direction, Map<String, List<String>> filterMap);

    WarehouseReceipt createWarehouseReceipt(WarehouseReceipt dto);

    Map<Long, Map<Long, Double>> getWarehouseBalance(LocalDate balanceDate);

    List<WarehouseReceipt> findWarehouseReceiptsByFilters(Map<String, List<String>> warehouseFilters);
}

