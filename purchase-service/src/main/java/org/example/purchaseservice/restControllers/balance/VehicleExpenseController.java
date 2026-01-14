package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseUpdateDTO;
import org.example.purchaseservice.services.impl.IVehicleExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Validated
public class VehicleExpenseController {
    private final IVehicleExpenseService vehicleExpenseService;

    @PreAuthorize("hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/expenses")
    public ResponseEntity<VehicleExpense> createVehicleExpense(
            @PathVariable @Positive Long vehicleId,
            @RequestBody @Valid @NonNull VehicleExpenseCreateDTO dto) {
        VehicleExpenseCreateDTO dtoWithVehicleId = new VehicleExpenseCreateDTO();
        dtoWithVehicleId.setVehicleId(vehicleId);
        dtoWithVehicleId.setFromAccountId(dto.getFromAccountId());
        dtoWithVehicleId.setCategoryId(dto.getCategoryId());
        dtoWithVehicleId.setAmount(dto.getAmount());
        dtoWithVehicleId.setCurrency(dto.getCurrency());
        dtoWithVehicleId.setExchangeRate(dto.getExchangeRate());
        dtoWithVehicleId.setConvertedAmount(dto.getConvertedAmount());
        dtoWithVehicleId.setDescription(dto.getDescription());
        VehicleExpense expense = vehicleExpenseService.createVehicleExpense(dtoWithVehicleId);
        return ResponseEntity.ok(expense);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{vehicleId}/expenses")
    public ResponseEntity<List<VehicleExpense>> getVehicleExpenses(@PathVariable @Positive Long vehicleId) {
        List<VehicleExpense> expenses = vehicleExpenseService.getExpensesByVehicleId(vehicleId);
        return ResponseEntity.ok(expenses);
    }

    @PreAuthorize("hasAuthority('declarant:create')")
    @PatchMapping("/expenses/{expenseId}")
    public ResponseEntity<VehicleExpense> updateVehicleExpense(
            @PathVariable @Positive Long expenseId,
            @RequestBody @Valid @NonNull VehicleExpenseUpdateDTO dto) {
        VehicleExpense expense = vehicleExpenseService.updateVehicleExpense(expenseId, dto);
        return ResponseEntity.ok(expense);
    }
}
