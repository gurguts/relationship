package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.dto.balance.InitialWarehouseBalanceDTO;
import org.example.purchaseservice.models.dto.balance.WarehouseBalanceAdjustmentDTO;
import org.example.purchaseservice.models.dto.balance.WarehouseBalanceUpdateRequest;
import org.example.purchaseservice.models.dto.balance.WarehouseProductBalanceDTO;
import org.example.purchaseservice.mappers.WarehouseBalanceMapper;
import org.example.purchaseservice.services.balance.IWarehouseProductBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse/balances")
@RequiredArgsConstructor
@Validated
public class WarehouseBalanceController {
    
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final WarehouseBalanceMapper warehouseBalanceMapper;

    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/initialize")
    public ResponseEntity<WarehouseProductBalanceDTO> setInitialBalance(
            @RequestBody @Valid @NonNull InitialWarehouseBalanceDTO dto) {
        
        log.info("Setting initial balance: warehouse={}, product={}, qty={}, price={}", 
                dto.getWarehouseId(), dto.getProductId(), dto.getInitialQuantity(), dto.getAveragePriceEur());
        
        WarehouseProductBalance balance = warehouseProductBalanceService.setInitialBalance(
                dto.getWarehouseId(),
                dto.getProductId(),
                dto.getInitialQuantity(),
                dto.getAveragePriceEur()
        );
        
        WarehouseProductBalanceDTO responseDTO = warehouseBalanceMapper.warehouseProductBalanceToWarehouseProductBalanceDTO(balance);
        return ResponseEntity.ok(responseDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{warehouseId}/product/{productId}")
    public ResponseEntity<WarehouseProductBalanceDTO> getProductBalance(
            @PathVariable @Positive Long warehouseId,
            @PathVariable @Positive Long productId) {
        
        WarehouseProductBalance balance = warehouseProductBalanceService.getBalance(warehouseId, productId);
        
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(warehouseBalanceMapper.warehouseProductBalanceToWarehouseProductBalanceDTO(balance));
    }

    @PreAuthorize("hasAuthority('warehouse:create')")
    @PatchMapping("/{warehouseId}/product/{productId}")
    public ResponseEntity<WarehouseProductBalanceDTO> updateProductBalance(
            @PathVariable @Positive Long warehouseId,
            @PathVariable @Positive Long productId,
            @RequestBody @Valid @NonNull WarehouseBalanceUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication != null ? (Long) authentication.getDetails() : null;

        WarehouseProductBalance updatedBalance = warehouseProductBalanceService.updateBalance(
                warehouseId,
                productId,
                request.getNewQuantity(),
                request.getNewTotalCostEur(),
                userId,
                request.getDescription()
        );

        return ResponseEntity.ok(warehouseBalanceMapper.warehouseProductBalanceToWarehouseProductBalanceDTO(updatedBalance));
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{warehouseId}")
    public ResponseEntity<List<WarehouseProductBalanceDTO>> getWarehouseBalances(
            @PathVariable @Positive Long warehouseId) {
        
        List<WarehouseProductBalance> balances = warehouseProductBalanceService.getWarehouseBalances(warehouseId);
        
        List<WarehouseProductBalanceDTO> dtos = balances.stream()
                .map(warehouseBalanceMapper::warehouseProductBalanceToWarehouseProductBalanceDTO)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{warehouseId}/product/{productId}/history")
    public ResponseEntity<List<WarehouseBalanceAdjustmentDTO>> getBalanceHistory(
            @PathVariable @Positive Long warehouseId,
            @PathVariable @Positive Long productId) {

        List<WarehouseBalanceAdjustment> adjustments = warehouseProductBalanceService
                .getBalanceAdjustments(warehouseId, productId);

        List<WarehouseBalanceAdjustmentDTO> dtos = adjustments.stream()
                .map(warehouseBalanceMapper::warehouseBalanceAdjustmentToWarehouseBalanceAdjustmentDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/active")
    public ResponseEntity<List<WarehouseProductBalanceDTO>> getAllActiveBalances() {
        List<WarehouseProductBalance> balances = warehouseProductBalanceService.getAllActiveBalances();
        
        List<WarehouseProductBalanceDTO> dtos = balances.stream()
                .map(warehouseBalanceMapper::warehouseProductBalanceToWarehouseProductBalanceDTO)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }
}

