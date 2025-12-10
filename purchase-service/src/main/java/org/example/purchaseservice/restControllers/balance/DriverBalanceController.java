package org.example.purchaseservice.restControllers.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.dto.balance.DriverProductBalanceDTO;
import org.example.purchaseservice.services.balance.DriverProductBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/driver/balances")
@RequiredArgsConstructor
public class DriverBalanceController {
    
    private final DriverProductBalanceService driverProductBalanceService;
    
    /**
     * Get balance for specific product of a driver
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/{driverId}/product/{productId}")
    public ResponseEntity<DriverProductBalanceDTO> getProductBalance(
            @PathVariable Long driverId,
            @PathVariable Long productId) {
        
        DriverProductBalance balance = driverProductBalanceService.getBalance(driverId, productId);
        
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(mapToDTO(balance));
    }
    
    /**
     * Get all balances for specific driver
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/{driverId}")
    public ResponseEntity<List<DriverProductBalanceDTO>> getDriverBalances(
            @PathVariable Long driverId) {
        
        List<DriverProductBalance> balances = driverProductBalanceService.getDriverBalances(driverId);
        
        List<DriverProductBalanceDTO> dtos = balances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get all active balances (quantity > 0) for all drivers
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/active")
    public ResponseEntity<List<DriverProductBalanceDTO>> getAllActiveBalances() {
        List<DriverProductBalance> balances = driverProductBalanceService.getAllActiveBalances();
        
        List<DriverProductBalanceDTO> dtos = balances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get balances for specific product across all drivers
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<DriverProductBalanceDTO>> getProductBalances(
            @PathVariable Long productId) {
        
        List<DriverProductBalance> balances = driverProductBalanceService.getProductBalances(productId);
        
        List<DriverProductBalanceDTO> dtos = balances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('profile:edit')")
    @PatchMapping("/{driverId}/product/{productId}/total-cost")
    public ResponseEntity<DriverProductBalanceDTO> updateTotalCostEur(
            @PathVariable Long driverId,
            @PathVariable Long productId,
            @RequestBody Map<String, BigDecimal> request) {
        
        BigDecimal newTotalCostEur = request.get("totalCostEur");
        if (newTotalCostEur == null) {
            return ResponseEntity.badRequest().build();
        }
        
        DriverProductBalance balance = driverProductBalanceService.updateTotalCostEur(
                driverId, productId, newTotalCostEur);
        
        return ResponseEntity.ok(mapToDTO(balance));
    }
    
    private DriverProductBalanceDTO mapToDTO(DriverProductBalance balance) {
        return DriverProductBalanceDTO.builder()
                .id(balance.getId())
                .driverId(balance.getDriverId())
                .productId(balance.getProductId())
                .quantity(balance.getQuantity())
                .averagePriceEur(balance.getAveragePriceEur())
                .totalCostEur(balance.getTotalCostEur())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }
}

