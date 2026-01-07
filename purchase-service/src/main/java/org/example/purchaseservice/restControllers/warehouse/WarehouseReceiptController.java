package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WarehouseReceiptMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.services.impl.IWarehouseReceiptService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Validated
public class WarehouseReceiptController {
    private final IWarehouseReceiptService warehouseReceiptService;
    private final WarehouseReceiptMapper warehouseReceiptMapper;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/receipts")
    public ResponseEntity<PageResponse<WarehouseReceiptDTO>> getWarehouseReceipts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) {
        Map<String, List<String>> filterMap;
        try {
            if (filters != null && !filters.isEmpty()) {
                filterMap = objectMapper.readValue(filters, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, List.class));
            } else {
                filterMap = Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Failed to parse filters: {}", filters, e);
            filterMap = Collections.emptyMap();
        }

        PageResponse<WarehouseReceiptDTO> result =
                warehouseReceiptService.getWarehouseReceipts(page, size, sort, direction, filterMap);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/receipts")
    public ResponseEntity<WarehouseReceiptDTO> createWarehouseReceipt(@RequestBody @Valid @NonNull WarehouseReceiptCreateDTO dto) {
        org.example.purchaseservice.models.warehouse.WarehouseReceipt result =
                warehouseReceiptService.createWarehouseReceipt(
                        warehouseReceiptMapper.warehouseReceiptCreateDTOToWarehouseReceipt(dto));

        WarehouseReceiptDTO warehouseReceiptDTO = warehouseReceiptMapper.warehouseReceiptToWarehouseReceiptDTO(result);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(warehouseReceiptDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(warehouseReceiptDTO);
    }

    @Deprecated
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/entries")
    public ResponseEntity<PageResponse<WarehouseReceiptDTO>> getWarehouseEntriesDeprecated(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) {
        return getWarehouseReceipts(page, size, sort, direction, filters);
    }

    @Deprecated
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/entries")
    public ResponseEntity<WarehouseReceiptDTO> createWarehouseEntryDeprecated(@RequestBody @Valid @NonNull WarehouseReceiptCreateDTO dto) {
        return createWarehouseReceipt(dto);
    }
}

