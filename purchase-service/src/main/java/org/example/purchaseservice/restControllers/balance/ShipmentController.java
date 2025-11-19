package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Shipment;
import org.example.purchaseservice.models.balance.ShipmentProduct;
import org.example.purchaseservice.models.dto.balance.AddProductToShipmentDTO;
import org.example.purchaseservice.models.dto.balance.ShipmentCreateDTO;
import org.example.purchaseservice.models.dto.balance.ShipmentDetailsDTO;
import org.example.purchaseservice.models.dto.balance.ShipmentProductUpdateDTO;
import org.example.purchaseservice.models.dto.balance.ShipmentUpdateDTO;
import org.example.purchaseservice.services.balance.ShipmentService;
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
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    
    private final ShipmentService shipmentService;
    
    /**
     * Create new shipment (vehicle)
     */
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping
    public ResponseEntity<ShipmentDetailsDTO> createShipment(@Valid @RequestBody ShipmentCreateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        
        Shipment shipment = shipmentService.createShipment(
                dto.getShipmentDate(),
                dto.getVehicleNumber(),
                dto.getInvoiceUa(),
                dto.getInvoiceEu(),
                dto.getDescription(),
                userId
        );
        
        ShipmentDetailsDTO detailsDTO = mapToDetailsDTO(shipment);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(shipment.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }
    
    /**
     * Get shipment details with product breakdown
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{shipmentId}")
    public ResponseEntity<ShipmentDetailsDTO> getShipmentDetails(@PathVariable Long shipmentId) {
        Shipment shipment = shipmentService.getShipment(shipmentId);
        
        if (shipment == null) {
            return ResponseEntity.notFound().build();
        }
        
        ShipmentDetailsDTO detailsDTO = mapToDetailsDTO(shipment);
        return ResponseEntity.ok(detailsDTO);
    }
    
    /**
     * Get all shipments for a specific date
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/by-date")
    public ResponseEntity<List<ShipmentDetailsDTO>> getShipmentsByDate(
            @RequestParam LocalDate date) {
        
        List<Shipment> shipments = shipmentService.getShipmentsByDate(date);
        
        List<ShipmentDetailsDTO> dtos = shipments.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get shipments for date range
     */
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/by-date-range")
    public ResponseEntity<List<ShipmentDetailsDTO>> getShipmentsByDateRange(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        
        List<Shipment> shipments = shipmentService.getShipmentsByDateRange(fromDate, toDate);
        
        List<ShipmentDetailsDTO> dtos = shipments.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Update shipment optional fields
     */
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PutMapping("/{shipmentId}")
    public ResponseEntity<ShipmentDetailsDTO> updateShipment(
            @PathVariable Long shipmentId,
            @RequestBody ShipmentUpdateDTO dto) {
        Shipment updated = shipmentService.updateShipment(shipmentId, dto);
        return ResponseEntity.ok(mapToDetailsDTO(updated));
    }
    
    /**
     * Add product to shipment
     */
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/{shipmentId}/products")
    public ResponseEntity<ShipmentDetailsDTO> addProductToShipment(
            @PathVariable Long shipmentId,
            @Valid @RequestBody AddProductToShipmentDTO dto) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        
        shipmentService.addProductToShipment(
                shipmentId,
                dto.getWarehouseId(),
                dto.getProductId(),
                dto.getQuantity(),
                userId
        );
        
        // Return updated shipment details
        Shipment shipment = shipmentService.getShipment(shipmentId);
        ShipmentDetailsDTO detailsDTO = mapToDetailsDTO(shipment);
        
        return ResponseEntity.ok(detailsDTO);
    }
    
    /**
     * Update product in shipment (quantity or total cost)
     */
    @PreAuthorize("hasAuthority('warehouse:create')")
    @PutMapping("/{shipmentId}/products/{shipmentProductId}")
    public ResponseEntity<ShipmentDetailsDTO> updateShipmentProduct(
            @PathVariable Long shipmentId,
            @PathVariable Long shipmentProductId,
            @Valid @RequestBody ShipmentProductUpdateDTO dto) {
        Shipment updated = shipmentService.updateShipmentProduct(
                shipmentId,
                shipmentProductId,
                dto.getQuantity(),
                dto.getTotalCostUah()
        );

        return ResponseEntity.ok(mapToDetailsDTO(updated));
    }
    
    /**
     * Delete shipment
     */
    @PreAuthorize("hasAuthority('warehouse:delete')")
    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<Void> deleteShipment(@PathVariable Long shipmentId) {
        shipmentService.deleteShipment(shipmentId);
        return ResponseEntity.noContent().build();
    }
    
    private ShipmentDetailsDTO mapToDetailsDTO(Shipment shipment) {
        // Get all products for this shipment
        List<ShipmentProduct> products = shipmentService.getShipmentProducts(shipment.getId());
        
        List<ShipmentDetailsDTO.ShipmentItemDTO> items = products.stream()
                .map(p -> ShipmentDetailsDTO.ShipmentItemDTO.builder()
                        .withdrawalId(p.getId()) // Using as item ID
                        .productId(p.getProductId())
                        .productName(null) // Will be enriched on frontend or via separate call
                        .warehouseId(p.getWarehouseId())
                        .quantity(p.getQuantity())
                        .unitPriceUah(p.getUnitPriceUah())
                        .totalCostUah(p.getTotalCostUah())
                        .withdrawalDate(p.getAddedAt() != null ? p.getAddedAt().toLocalDate() : shipment.getShipmentDate())
                        .build())
                .collect(Collectors.toList());
        
        return ShipmentDetailsDTO.builder()
                .id(shipment.getId())
                .shipmentDate(shipment.getShipmentDate())
                .vehicleNumber(shipment.getVehicleNumber())
                .invoiceUa(shipment.getInvoiceUa())
                .invoiceEu(shipment.getInvoiceEu())
                .description(shipment.getDescription())
                .totalCostUah(shipment.getTotalCostUah())
                .userId(shipment.getUserId())
                .createdAt(shipment.getCreatedAt())
                .items(items)
                .build();
    }
}

