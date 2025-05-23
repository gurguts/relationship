package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.services.impl.IWarehouseEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseEntryController {
    private final IWarehouseEntryService warehouseEntryService;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/entries")
    public ResponseEntity<PageResponse<WarehouseEntryDTO>> getWarehouseEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {
        Map<String, List<String>> filterMap = filters != null && !filters.isEmpty()
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {
        })
                : Collections.emptyMap();

        PageResponse<WarehouseEntryDTO> result =
                warehouseEntryService.getWarehouseEntries(page, size, sort, direction, filterMap);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/entries")
    public ResponseEntity<WarehouseEntryDTO> createWarehouseEntry(@RequestBody WarehouseEntryDTO dto) {
        WarehouseEntryDTO result = warehouseEntryService.createWarehouseEntry(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PreAuthorize("hasAuthority('warehouse:edit')")
    @PatchMapping("/entries/{warehouseId}")
    public ResponseEntity<Void> updateWarehouseEntry(@PathVariable Long warehouseId,
                                                     @RequestBody UpdateWarehouseEntryDTO dto) {
        warehouseEntryService.updateWarehouseEntry(warehouseId, dto.quantity());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/balance")
    public ResponseEntity<BalanceWarehouseDTO> getBalance() {
        return ResponseEntity.ok(warehouseEntryService.getWarehouseBalance());
    }
}


