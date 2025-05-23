package org.example.purchaseservice.restControllers.purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.mappers.PurchaseMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.PurchaseModalDTO;
import org.example.purchaseservice.models.dto.purchase.PurchasePageDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseWarehouseDTO;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PurchaseSearchController {
    private final IPurchaseSearchService purchaseSearchService;
    private final ObjectMapper objectMapper;
    private final PurchaseMapper purchaseMapper;

    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<PurchasePageDTO>> searchPurchase(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "direction", defaultValue = "ASC") String direction,
            @RequestParam(name = "filters", required = false) String filters) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Map<String, List<String>> filterParams;

        try {
            if (filters != null && !filters.isEmpty()) {
                filterParams = objectMapper.readValue(filters, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, List.class));
            } else {
                filterParams = Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Failed to parse filters: {}", filters, e);
            throw new PurchaseException("INVALID_FILTERS", "Invalid filters format");
        }

        PageResponse<PurchasePageDTO> result = purchaseSearchService.searchPurchase(query, pageable, filterParams);

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<PurchaseModalDTO>> getSalesByClientId(@PathVariable("clientId") Long clientId) {
        List<Purchase> purchaseRecords = purchaseSearchService.getPurchasesByClientId(clientId);
        List<PurchaseModalDTO> purchaseDTOs = purchaseRecords.stream()
                .map(purchaseMapper::purchaseToPurchaseModalDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(purchaseDTOs);
    }

    @GetMapping("/warehouse")
    public ResponseEntity<List<PurchaseWarehouseDTO>> getPurchasesByFilters(@RequestParam Map<String, String> filters) {

        Map<String, List<String>> listFilters = new HashMap<>();
        filters.forEach((key, value) -> listFilters.put(key, List.of(value)));


        List<PurchaseWarehouseDTO> purchaseList = purchaseSearchService.searchForWarehouse(listFilters).stream()
                .map(purchaseMapper::purchaseToPurchaseWarehouseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(purchaseList);
    }
}
