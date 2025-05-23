package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.WarehouseEntry;
import org.example.purchaseservice.models.dto.warehouse.BalanceWarehouseDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseEntryDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IWarehouseEntryService {
    PageResponse<WarehouseEntryDTO> getWarehouseEntries(int page, int size, String sort, String direction, Map<String, List<String>> filterMap);

    WarehouseEntryDTO createWarehouseEntry(WarehouseEntryDTO dto);

    void updateWarehouseEntry(Long warehouseId, BigDecimal quantity);

    BalanceWarehouseDTO getWarehouseBalance();

    List<WarehouseEntry> findWarehouseEntriesByFilters(Map<String, List<String>> warehouseFilters);
}
