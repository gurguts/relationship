package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.dto.balance.DriverProductBalanceDTO;
import org.example.purchaseservice.models.dto.balance.UpdateTotalCostRequest;
import org.example.purchaseservice.mappers.DriverProductBalanceMapper;
import org.example.purchaseservice.services.balance.DriverProductBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/driver/balances")
@RequiredArgsConstructor
@Validated
public class DriverBalanceController {
    private final DriverProductBalanceService driverProductBalanceService;
    private final DriverProductBalanceMapper driverProductBalanceMapper;

    @GetMapping("/{driverId}/product/{productId}")
    public ResponseEntity<DriverProductBalanceDTO> getProductBalance(
            @PathVariable @Positive Long driverId,
            @PathVariable @Positive Long productId) {
        DriverProductBalance balance = driverProductBalanceService.getBalance(driverId, productId);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(driverProductBalanceMapper.driverProductBalanceToDTO(balance));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<List<DriverProductBalanceDTO>> getDriverBalances(
            @PathVariable @Positive Long driverId) {
        List<DriverProductBalance> balances = driverProductBalanceService.getDriverBalances(driverId);
        List<DriverProductBalanceDTO> dtos = balances.stream()
                .map(driverProductBalanceMapper::driverProductBalanceToDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<DriverProductBalanceDTO>> getAllActiveBalances() {
        List<DriverProductBalance> balances = driverProductBalanceService.getAllActiveBalances();
        List<DriverProductBalanceDTO> dtos = balances.stream()
                .map(driverProductBalanceMapper::driverProductBalanceToDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<DriverProductBalanceDTO>> getProductBalances(
            @PathVariable @Positive Long productId) {
        List<DriverProductBalance> balances = driverProductBalanceService.getProductBalances(productId);
        List<DriverProductBalanceDTO> dtos = balances.stream()
                .map(driverProductBalanceMapper::driverProductBalanceToDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('profile:edit')")
    @PatchMapping("/{driverId}/product/{productId}/total-cost")
    public ResponseEntity<DriverProductBalanceDTO> updateTotalCostEur(
            @PathVariable @Positive Long driverId,
            @PathVariable @Positive Long productId,
            @RequestBody @Valid @NonNull UpdateTotalCostRequest request) {
        DriverProductBalance balance = driverProductBalanceService.updateTotalCostEur(
                driverId, productId, request.getTotalCostEur());
        return ResponseEntity.ok(driverProductBalanceMapper.driverProductBalanceToDTO(balance));
    }
}
