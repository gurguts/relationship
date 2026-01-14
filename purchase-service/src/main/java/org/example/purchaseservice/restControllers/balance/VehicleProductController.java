package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.dto.balance.AddProductToVehicleDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleProductUpdateDTO;
import org.example.purchaseservice.mappers.VehicleMapper;
import org.example.purchaseservice.services.balance.IVehicleService;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Validated
public class VehicleProductController {
    private final IVehicleService vehicleService;
    private final VehicleMapper vehicleMapper;

    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/products")
    public ResponseEntity<VehicleDetailsDTO> addProductToVehicle(
            @PathVariable @Positive Long vehicleId,
            @RequestBody @Valid @NonNull AddProductToVehicleDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Vehicle vehicle = vehicleService.addProductToVehicle(
                vehicleId,
                dto.getWarehouseId(),
                dto.getProductId(),
                dto.getQuantity(),
                userId
        );
        VehicleDetailsDTO detailsDTO = vehicleMapper.vehicleToVehicleDetailsDTO(vehicle);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:edit') or hasAuthority('declarant:edit')")
    @PatchMapping("/{vehicleId}/products/{vehicleProductId}")
    public ResponseEntity<VehicleDetailsDTO> updateVehicleProduct(
            @PathVariable @Positive Long vehicleId,
            @PathVariable @Positive Long vehicleProductId,
            @RequestBody @Valid @NonNull VehicleProductUpdateDTO dto) {
        Vehicle updated = vehicleService.updateVehicleProduct(
                vehicleId,
                vehicleProductId,
                dto.getQuantity(),
                dto.getTotalCostEur()
        );
        return ResponseEntity.ok(vehicleMapper.vehicleToVehicleDetailsDTO(updated));
    }
}
