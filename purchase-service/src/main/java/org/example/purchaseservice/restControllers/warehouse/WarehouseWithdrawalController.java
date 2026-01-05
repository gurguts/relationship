package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.mappers.WarehouseWithdrawalMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalDTO;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalCreateDTO;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Validated
public class WarehouseWithdrawalController {
    private final IWarehouseWithdrawService warehouseWithdrawService;
    private final WarehouseWithdrawalMapper warehouseWithdrawalMapper;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PostMapping("/withdraw")
    public ResponseEntity<WarehouseWithdrawalDTO> createWithdrawal(@RequestBody @Valid @NonNull WithdrawalCreateDTO request) {
        org.example.purchaseservice.models.warehouse.WarehouseWithdrawal warehouseWithdrawal =
                warehouseWithdrawService.createWithdrawal(
                        warehouseWithdrawalMapper.withdrawalCreateDTOToWarehouseWithdrawal(request));

        WarehouseWithdrawalDTO warehouseWithdrawalDTO =
                warehouseWithdrawalMapper.warehouseWithdrawalToWarehouseWithdrawalDTO(warehouseWithdrawal);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(warehouseWithdrawalDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(warehouseWithdrawalDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PatchMapping("/withdraw/{id}")
    public ResponseEntity<WarehouseWithdrawalDTO> updateWithdrawal(
            @PathVariable @Positive Long id,
            @RequestBody @Valid @NonNull WarehouseWithdrawalUpdateDTO warehouseWithdrawalUpdateDTO) {
        org.example.purchaseservice.models.warehouse.WarehouseWithdrawal request =
                warehouseWithdrawalMapper.withdrawalUpdateDTOToWarehouseWithdrawal(warehouseWithdrawalUpdateDTO);

        org.example.purchaseservice.models.warehouse.WarehouseWithdrawal updated = warehouseWithdrawService.updateWithdrawal(id, request);

        return updated != null
                ? ResponseEntity.ok(warehouseWithdrawalMapper.warehouseWithdrawalToWarehouseWithdrawalDTO(updated))
                : ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<Void> deleteWithdrawal(@PathVariable @Positive Long id) {
        warehouseWithdrawService.deleteWithdrawal(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/withdrawals")
    public ResponseEntity<PageResponse<WithdrawalDTO>> getWithdrawals(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) int size,
            @RequestParam(defaultValue = "withdrawalDate") String sort,
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

        PageResponse<WithdrawalDTO> result =
                warehouseWithdrawService.getWithdrawals(page, size, sort, direction, filterMap);

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/withdrawal-reasons")
    public ResponseEntity<List<WithdrawalReason>> getAllWithdrawalReasons() {
        List<WithdrawalReason> reasons = warehouseWithdrawService.getAllWithdrawalReasons();
        return ResponseEntity.ok(reasons);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/withdrawal-reasons/purpose/{purpose}")
    public ResponseEntity<List<WithdrawalReason>> getWithdrawalReasonsByPurpose(@PathVariable String purpose) {
        WithdrawalReason.Purpose purposeEnum;
        try {
            purposeEnum = WithdrawalReason.Purpose.valueOf(purpose.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PurchaseException("INVALID_PURPOSE", "Invalid purpose: " + purpose);
        }
        List<WithdrawalReason> reasons = warehouseWithdrawService.getWithdrawalReasonsByPurpose(purposeEnum);
        return ResponseEntity.ok(reasons);
    }
}
