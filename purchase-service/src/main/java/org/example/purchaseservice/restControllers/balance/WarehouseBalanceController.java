package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.WarehouseBalanceAdjustment;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.dto.balance.InitialWarehouseBalanceDTO;
import org.example.purchaseservice.models.dto.balance.WarehouseBalanceAdjustmentDTO;
import org.example.purchaseservice.models.dto.balance.WarehouseBalanceUpdateRequest;
import org.example.purchaseservice.models.dto.balance.WarehouseProductBalanceDTO;
import org.example.purchaseservice.services.balance.WarehouseProductBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse/balances")
@RequiredArgsConstructor
public class WarehouseBalanceController {
    
    private final WarehouseProductBalanceService warehouseProductBalanceService;
    
    /**
     * Set initial warehouse balance for a product
     * Use this to set starting balance when implementing the system
     */
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/initialize")
    public ResponseEntity<WarehouseProductBalanceDTO> setInitialBalance(
            @Valid @RequestBody InitialWarehouseBalanceDTO dto) {
        
        log.info("Setting initial balance: warehouse={}, product={}, qty={}, price={}", 
                dto.getWarehouseId(), dto.getProductId(), dto.getInitialQuantity(), dto.getAveragePriceEur());
        
        WarehouseProductBalance balance = warehouseProductBalanceService.setInitialBalance(
                dto.getWarehouseId(),
                dto.getProductId(),
                dto.getInitialQuantity(),
                dto.getAveragePriceEur()
        );
        
        WarehouseProductBalanceDTO responseDTO = mapToDTO(balance);
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * Get balance for specific product on warehouse
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{warehouseId}/product/{productId}")
    public ResponseEntity<WarehouseProductBalanceDTO> getProductBalance(
            @PathVariable Long warehouseId,
            @PathVariable Long productId) {
        
        WarehouseProductBalance balance = warehouseProductBalanceService.getBalance(warehouseId, productId);
        
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(mapToDTO(balance));
    }
    
    /**
     * Update warehouse balance for specific product
     */
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PutMapping("/{warehouseId}/product/{productId}")
    public ResponseEntity<WarehouseProductBalanceDTO> updateProductBalance(
            @PathVariable Long warehouseId,
            @PathVariable Long productId,
            @RequestBody WarehouseBalanceUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (authentication != null && authentication.getDetails() instanceof Long) {
            userId = (Long) authentication.getDetails();
        }

        WarehouseProductBalance updatedBalance = warehouseProductBalanceService.updateBalance(
                warehouseId,
                productId,
                request.getNewQuantity(),
                request.getNewTotalCostEur(),
                userId,
                request.getDescription()
        );

        return ResponseEntity.ok(mapToDTO(updatedBalance));
    }
    
    /**
     * Get all balances for specific warehouse
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{warehouseId}")
    public ResponseEntity<List<WarehouseProductBalanceDTO>> getWarehouseBalances(
            @PathVariable Long warehouseId) {
        
        List<WarehouseProductBalance> balances = warehouseProductBalanceService.getWarehouseBalances(warehouseId);
        
        List<WarehouseProductBalanceDTO> dtos = balances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get balance adjustment history
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{warehouseId}/product/{productId}/history")
    public ResponseEntity<List<WarehouseBalanceAdjustmentDTO>> getBalanceHistory(
            @PathVariable Long warehouseId,
            @PathVariable Long productId) {

        List<WarehouseBalanceAdjustment> adjustments = warehouseProductBalanceService
                .getBalanceAdjustments(warehouseId, productId);

        List<WarehouseBalanceAdjustmentDTO> dtos = adjustments.stream()
                .map(this::mapAdjustmentToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get all active balances (quantity > 0)
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/active")
    public ResponseEntity<List<WarehouseProductBalanceDTO>> getAllActiveBalances() {
        List<WarehouseProductBalance> balances = warehouseProductBalanceService.getAllActiveBalances();
        
        List<WarehouseProductBalanceDTO> dtos = balances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    private WarehouseProductBalanceDTO mapToDTO(WarehouseProductBalance balance) {
        return WarehouseProductBalanceDTO.builder()
                .id(balance.getId())
                .warehouseId(balance.getWarehouseId())
                .productId(balance.getProductId())
                .quantity(balance.getQuantity())
                .averagePriceEur(balance.getAveragePriceEur())
                .totalCostEur(balance.getTotalCostEur())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }

    private WarehouseBalanceAdjustmentDTO mapAdjustmentToDTO(WarehouseBalanceAdjustment adjustment) {
        return WarehouseBalanceAdjustmentDTO.builder()
                .id(adjustment.getId())
                .warehouseId(adjustment.getWarehouseId())
                .productId(adjustment.getProductId())
                .previousQuantity(adjustment.getPreviousQuantity())
                .newQuantity(adjustment.getNewQuantity())
                .previousTotalCostEur(adjustment.getPreviousTotalCostEur())
                .newTotalCostEur(adjustment.getNewTotalCostEur())
                .previousAveragePriceEur(adjustment.getPreviousAveragePriceEur())
                .newAveragePriceEur(adjustment.getNewAveragePriceEur())
                .adjustmentType(adjustment.getAdjustmentType())
                .description(adjustment.getDescription())
                .userId(adjustment.getUserId())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}

