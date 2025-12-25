package org.example.purchaseservice.restControllers.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.AddProductToVehicleDTO;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleProductUpdateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.services.balance.VehicleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Validated
public class VehicleController {
    
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;
    private final org.example.purchaseservice.services.balance.VehicleExportService vehicleExportService;
    
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
                dto.getIsOurVehicle(),
                dto.getSender(),
                dto.getReceiver(),
                dto.getDestinationCountry(),
                dto.getDestinationPlace(),
                dto.getProduct(),
                dto.getProductQuantity(),
                dto.getDeclarationNumber(),
                dto.getTerminal(),
                dto.getDriverFullName(),
                dto.getEur1(),
                dto.getFito(),
                dto.getCustomsDate(),
                dto.getCustomsClearanceDate(),
                dto.getUnloadingDate(),
                dto.getInvoiceUaDate(),
                dto.getInvoiceUaPricePerTon(),
                dto.getInvoiceEuDate(),
                dto.getInvoiceEuPricePerTon(),
                dto.getReclamation(),
                dto.getCarrierId()
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
    
    @PostMapping("/ids")
    public ResponseEntity<List<Map<Long, String>>> getVehiclesByIds(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Vehicle> vehicles = vehicleService.getVehiclesByIds(ids);
        List<Map<Long, String>> result = vehicles.stream()
                .map(v -> Map.of(v.getId(), v.getVehicleNumber() != null ? v.getVehicleNumber() : ""))
                .toList();
        return ResponseEntity.ok(result);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehiclesToExcel(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "filters", required = false) String filters) throws java.io.IOException {

        Map<String, List<String>> filterParams;
        if (filters != null && !filters.isEmpty()) {
            filterParams = objectMapper.readValue(filters, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, List.class));
        } else {
            filterParams = Collections.emptyMap();
        }

        byte[] excelData = vehicleExportService.exportToExcel(query, filterParams);

        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=vehicles.xlsx")
                .body(excelData);
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
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/cost")
    public ResponseEntity<Void> updateVehicleCost(
            @PathVariable Long vehicleId,
            @RequestParam java.math.BigDecimal amountEur,
            @RequestParam String operation) {
        if ("add".equalsIgnoreCase(operation)) {
            vehicleService.addWithdrawalCost(vehicleId, amountEur);
        } else if ("subtract".equalsIgnoreCase(operation)) {
            vehicleService.subtractWithdrawalCost(vehicleId, amountEur);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<VehicleDetailsDTO>> searchVehicles(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction,
            @RequestParam(name = "filters", required = false) String filters) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Map<String, List<String>> filterParams;
        try {
            if (filters != null && !filters.isEmpty()) {
                filterParams = objectMapper.readValue(filters, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, List.class));
            } else {
                filterParams = Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Failed to parse filters: {}", filters, e);
            filterParams = Collections.emptyMap();
        }

        PageResponse<VehicleDetailsDTO> result = vehicleService.searchVehicles(query, pageable, filterParams);
        return ResponseEntity.ok(result);
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
        
        CarrierDetailsDTO carrierDTO = null;
        if (vehicle.getCarrier() != null) {
            Carrier carrier = vehicle.getCarrier();
            carrierDTO = CarrierDetailsDTO.builder()
                    .id(carrier.getId())
                    .companyName(carrier.getCompanyName())
                    .registrationAddress(carrier.getRegistrationAddress())
                    .phoneNumber(carrier.getPhoneNumber())
                    .code(carrier.getCode())
                    .account(carrier.getAccount())
                    .createdAt(carrier.getCreatedAt())
                    .updatedAt(carrier.getUpdatedAt())
                    .build();
        }
        
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
                .sender(vehicle.getSender())
                .receiver(vehicle.getReceiver())
                .destinationCountry(vehicle.getDestinationCountry())
                .destinationPlace(vehicle.getDestinationPlace())
                .product(vehicle.getProduct())
                .productQuantity(vehicle.getProductQuantity())
                .declarationNumber(vehicle.getDeclarationNumber())
                .terminal(vehicle.getTerminal())
                .driverFullName(vehicle.getDriverFullName())
                .isOurVehicle(vehicle.getIsOurVehicle())
                .eur1(vehicle.getEur1())
                .fito(vehicle.getFito())
                .customsDate(vehicle.getCustomsDate())
                .customsClearanceDate(vehicle.getCustomsClearanceDate())
                .unloadingDate(vehicle.getUnloadingDate())
                .invoiceUaDate(vehicle.getInvoiceUaDate())
                .invoiceUaPricePerTon(vehicle.getInvoiceUaPricePerTon())
                .invoiceUaTotalPrice(vehicle.getInvoiceUaTotalPrice())
                .invoiceEuDate(vehicle.getInvoiceEuDate())
                .invoiceEuPricePerTon(vehicle.getInvoiceEuPricePerTon())
                .invoiceEuTotalPrice(vehicle.getInvoiceEuTotalPrice())
                .reclamation(vehicle.getReclamation())
                .carrier(carrierDTO)
                .items(items)
                .build();
    }
}

