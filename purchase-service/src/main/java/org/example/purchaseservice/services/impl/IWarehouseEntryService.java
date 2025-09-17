package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.warehouse.WarehouseEntry;
import org.example.purchaseservice.models.dto.warehouse.WarehouseEntryDTO;
import org.example.purchaseservice.models.dto.warehouse.UpdateWarehouseEntryDTO;

import java.util.List;
import java.util.Map;

public interface IWarehouseEntryService {
    PageResponse<WarehouseEntryDTO> getWarehouseEntries(int page, int size, String sort, String direction, Map<String, List<String>> filterMap);

    WarehouseEntry createWarehouseEntry(WarehouseEntry dto);

    void updateWarehouseEntry(Long warehouseId, UpdateWarehouseEntryDTO updateDTO);

    Map<Long, Map<Long, Double>> getWarehouseBalance();

    List<WarehouseEntry> findWarehouseEntriesByFilters(Map<String, List<String>> warehouseFilters);
}
