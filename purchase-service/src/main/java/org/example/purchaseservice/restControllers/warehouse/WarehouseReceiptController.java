package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WarehouseReceiptMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.services.impl.IWarehouseReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class WarehouseReceiptController {
    private final IWarehouseReceiptService warehouseReceiptService;
    private final WarehouseReceiptMapper warehouseReceiptMapper;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/receipts")
    public ResponseEntity<PageResponse<WarehouseReceiptDTO>> getWarehouseReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {
        Map<String, List<String>> filterMap = filters != null && !filters.isEmpty()
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {
        })
                : Collections.emptyMap();

        PageResponse<WarehouseReceiptDTO> result =
                warehouseReceiptService.getWarehouseReceipts(page, size, sort, direction, filterMap);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/receipts")
    public ResponseEntity<WarehouseReceiptDTO> createWarehouseReceipt(@RequestBody WarehouseReceiptCreateDTO dto) {
        WarehouseReceipt result =
                warehouseReceiptService.createWarehouseReceipt(
                        warehouseReceiptMapper.warehouseReceiptCreateDTOToWarehouseReceipt(dto));

        WarehouseReceiptDTO warehouseReceiptDTO = warehouseReceiptMapper.warehouseReceiptToWarehouseReceiptDTO(result);


        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(warehouseReceiptDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(warehouseReceiptDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/balance")
    public ResponseEntity<BalanceWarehouseDTO> getBalance(
            @RequestParam(required = false) String balanceDate) {

        LocalDate date = balanceDate != null ? LocalDate.parse(balanceDate) : LocalDate.now();

        Map<Long, Map<Long, Double>> balanceByWarehouseAndProduct = warehouseReceiptService.getWarehouseBalance(date);

        BalanceWarehouseDTO balanceWarehouseDTO = BalanceWarehouseDTO.builder()
                .balanceByWarehouseAndProduct(balanceByWarehouseAndProduct)
                .build();

        return ResponseEntity.ok(balanceWarehouseDTO);
    }
    
    // Keep old endpoint /entries for backward compatibility (will be deprecated)
    @Deprecated
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/entries")
    public ResponseEntity<PageResponse<WarehouseReceiptDTO>> getWarehouseEntriesDeprecated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {
        return getWarehouseReceipts(page, size, sort, direction, filters);
    }

    @Deprecated
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/entries")
    public ResponseEntity<WarehouseReceiptDTO> createWarehouseEntryDeprecated(@RequestBody WarehouseReceiptCreateDTO dto) {
        return createWarehouseReceipt(dto);
    }
}

