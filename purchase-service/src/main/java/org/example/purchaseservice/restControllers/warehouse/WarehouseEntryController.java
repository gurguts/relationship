package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WarehouseEntryMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.models.warehouse.WarehouseEntry;
import org.example.purchaseservice.services.impl.IWarehouseEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseEntryController {
    private final IWarehouseEntryService warehouseEntryService;
    private final WarehouseEntryMapper warehouseEntryMapper;

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
    public ResponseEntity<WarehouseEntryDTO> createWarehouseEntry(@RequestBody WarehouseEntryCreateDTO dto) {
        WarehouseEntry result =
                warehouseEntryService.createWarehouseEntry(
                        warehouseEntryMapper.warehouseEntryCreateDTOToWarehouseEntry(dto));

        WarehouseEntryDTO warehouseEntryDTO = warehouseEntryMapper.warehouseEntryToWarehouseEntryDTO(result);


        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(warehouseEntryDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(warehouseEntryDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:edit')")
    @PatchMapping("/entries/{warehouseId}")
    public ResponseEntity<Void> updateWarehouseEntry(@PathVariable Long warehouseId,
                                                     @RequestBody Map<String, BigDecimal> request) {
        BigDecimal newQuantity = request.get("quantity");
        warehouseEntryService.updateWarehouseEntry(warehouseId, newQuantity);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/balance")
    public ResponseEntity<BalanceWarehouseDTO> getBalance(
            @RequestParam(required = false) String balanceDate) {
        
        LocalDate date = balanceDate != null ? LocalDate.parse(balanceDate) : LocalDate.now();
        
        Map<Long, Map<Long, Double>> balanceByWarehouseAndProduct = warehouseEntryService.getWarehouseBalance(date);

        BalanceWarehouseDTO balanceWarehouseDTO = BalanceWarehouseDTO.builder()
                .balanceByWarehouseAndProduct(balanceByWarehouseAndProduct)
                .build();

        return ResponseEntity.ok(balanceWarehouseDTO);
    }
}


