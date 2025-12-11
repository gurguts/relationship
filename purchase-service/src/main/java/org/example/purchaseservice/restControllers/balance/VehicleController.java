package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.AddProductToVehicleDTO;
import org.example.purchaseservice.models.dto.balance.VehicleCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleProductUpdateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.services.balance.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    
    private final VehicleService vehicleService;
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleDetailsDTO> createVehicle(@Valid @RequestBody VehicleCreateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        
        Vehicle vehicle = vehicleService.createVehicle(
                dto.getShipmentDate(),
                dto.getVehicleNumber(),
                dto.getInvoiceUa(),
                dto.getInvoiceEu(),
                dto.getDescription(),
                userId,
                dto.getIsOurVehicle()
        );
        
        VehicleDetailsDTO detailsDTO = mapToDetailsDTO(vehicle);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vehicle.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleDetailsDTO> getVehicleDetails(@PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);
        
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }
        
        VehicleDetailsDTO detailsDTO = mapToDetailsDTO(vehicle);
        return ResponseEntity.ok(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/by-date")
    public ResponseEntity<List<VehicleDetailsDTO>> getVehiclesByDate(
            @RequestParam LocalDate date) {
        
        List<Vehicle> vehicles = vehicleService.getVehiclesByDate(date);
        
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/by-date-range")
    public ResponseEntity<List<VehicleDetailsDTO>> getOurVehiclesByDateRange(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        
        List<Vehicle> vehicles = vehicleService.getOurVehiclesByDateRange(fromDate, toDate);
        
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/all/by-date-range")
    public ResponseEntity<List<VehicleDetailsDTO>> getAllVehiclesByDateRange(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        
        List<Vehicle> vehicles = vehicleService.getAllVehiclesByDateRange(fromDate, toDate);
        
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleDetailsDTO> updateVehicle(
            @PathVariable Long vehicleId,
            @RequestBody VehicleUpdateDTO dto) {
        Vehicle updated = vehicleService.updateVehicle(vehicleId, dto);
        return ResponseEntity.ok(mapToDetailsDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/products")
    public ResponseEntity<VehicleDetailsDTO> addProductToVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody AddProductToVehicleDTO dto) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        
        vehicleService.addProductToVehicle(
                vehicleId,
                dto.getWarehouseId(),
                dto.getProductId(),
                dto.getQuantity(),
                userId
        );
        
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);
        VehicleDetailsDTO detailsDTO = mapToDetailsDTO(vehicle);
        
        return ResponseEntity.ok(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PutMapping("/{vehicleId}/products/{vehicleProductId}")
    public ResponseEntity<VehicleDetailsDTO> updateVehicleProduct(
            @PathVariable Long vehicleId,
            @PathVariable Long vehicleProductId,
            @Valid @RequestBody VehicleProductUpdateDTO dto) {
        Vehicle updated = vehicleService.updateVehicleProduct(
                vehicleId,
                vehicleProductId,
                dto.getQuantity(),
                dto.getTotalCostEur()
        );

        return ResponseEntity.ok(mapToDetailsDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('warehouse:delete') or hasAuthority('declarant:delete')")
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
    
    private VehicleDetailsDTO mapToDetailsDTO(Vehicle vehicle) {
        List<VehicleProduct> products = vehicleService.getVehicleProducts(vehicle.getId());
        
        List<VehicleDetailsDTO.VehicleItemDTO> items = products.stream()
                .map(p -> VehicleDetailsDTO.VehicleItemDTO.builder()
                        .withdrawalId(p.getId())
                        .productId(p.getProductId())
                        .productName(null)
                        .warehouseId(p.getWarehouseId())
                        .quantity(p.getQuantity())
                        .unitPriceEur(p.getUnitPriceEur())
                        .totalCostEur(p.getTotalCostEur())
                        .withdrawalDate(p.getAddedAt() != null ? p.getAddedAt().toLocalDate() : vehicle.getShipmentDate())
                        .build())
                .collect(Collectors.toList());
        
        return VehicleDetailsDTO.builder()
                .id(vehicle.getId())
                .shipmentDate(vehicle.getShipmentDate())
                .vehicleNumber(vehicle.getVehicleNumber())
                .invoiceUa(vehicle.getInvoiceUa())
                .invoiceEu(vehicle.getInvoiceEu())
                .description(vehicle.getDescription())
                .totalCostEur(vehicle.getTotalCostEur())
                .userId(vehicle.getUserId())
                .createdAt(vehicle.getCreatedAt())
                .items(items)
                .build();
    }
}

