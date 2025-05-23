package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalRequestDTO;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
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
public class WarehouseWithdrawalController {
    private final IWarehouseWithdrawService warehouseEntryService;

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PostMapping("/withdraw")
    public ResponseEntity<WarehouseWithdrawal> createWithdrawal(@RequestBody WithdrawalRequestDTO request) {
        return ResponseEntity.ok(warehouseEntryService.createWithdrawal(request));
    }

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PutMapping("/withdraw/{id}")
    public ResponseEntity<WarehouseWithdrawal> updateWithdrawal(
            @PathVariable Long id,
            @RequestBody WarehouseWithdrawalUpdateDTO request) {
        return ResponseEntity.ok(warehouseEntryService.updateWithdrawal(id, request));
    }

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<Void> deleteWithdrawal(@PathVariable Long id) {
        warehouseEntryService.deleteWithdrawal(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/withdrawals")
    public ResponseEntity<PageResponse<WithdrawalDTO>> getWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "withdrawalDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {
        Map<String, List<String>> filterMap = filters != null && !filters.isEmpty()
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {
        })
                : Collections.emptyMap();

        PageResponse<WithdrawalDTO> result =
                warehouseEntryService.getWithdrawals(page, size, sort, direction, filterMap);
        return ResponseEntity.ok(result);
    }
}
