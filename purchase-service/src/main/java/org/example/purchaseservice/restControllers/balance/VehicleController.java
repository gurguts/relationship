package org.example.purchaseservice.restControllers.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.UpdateVehicleCostRequest;
import org.example.purchaseservice.models.dto.balance.VehicleCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.mappers.VehicleMapper;
import org.example.purchaseservice.services.balance.IVehicleService;
import org.example.purchaseservice.services.impl.IVehicleExportService;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Validated
public class VehicleController {
    private final IVehicleService vehicleService;
    private final ObjectMapper objectMapper;
    private final IVehicleExportService vehicleExportService;
    private final VehicleMapper vehicleMapper;
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleDetailsDTO> createVehicle(@RequestBody @Valid @NonNull VehicleCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        assert userId != null;
        Vehicle vehicle = vehicleMapper.vehicleCreateDTOToVehicle(dto, userId);
        Vehicle created = vehicleService.createVehicle(vehicle);
        VehicleDetailsDTO detailsDTO = vehicleMapper.vehicleToVehicleDetailsDTO(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleDetailsDTO> getVehicleDetails(@PathVariable @Positive Long vehicleId) {
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);
        VehicleDetailsDTO detailsDTO = vehicleMapper.vehicleToVehicleDetailsDTO(vehicle);
        return ResponseEntity.ok(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @PostMapping("/ids")
    public ResponseEntity<List<Map<Long, String>>> getVehiclesByIds(@RequestBody @NonNull List<Long> ids) {
        if (ids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Vehicle> vehicles = vehicleService.getVehiclesByIds(ids);
        List<Map<Long, String>> result = vehicles.stream()
                .map(v -> Map.of(v.getId(), v.getVehicleNumber() != null ? v.getVehicleNumber() : ""))
                .toList();
        return ResponseEntity.ok(result);
    }
    
    @PreAuthorize("hasAuthority('declarant:excel')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehiclesToExcel(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "filters", required = false) String filters) {
        try {
            Map<String, List<String>> filterParams = parseFilters(filters);
            byte[] excelData = vehicleExportService.exportToExcel(query, filterParams);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=vehicles.xlsx")
                    .body(excelData);
        } catch (IOException e) {
            log.error("Error exporting vehicles to Excel", e);
            throw new PurchaseException("EXPORT_FAILED", "Failed to export vehicles to Excel: " + e.getMessage());
        }
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/by-date")
    public ResponseEntity<List<VehicleDetailsDTO>> getVehiclesByDate(
            @RequestParam @NonNull LocalDate date) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByDate(date);
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(vehicleMapper::vehicleToVehicleDetailsDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/by-date-range")
    public ResponseEntity<List<VehicleDetailsDTO>> getOurVehiclesByDateRange(
            @RequestParam @NonNull LocalDate fromDate,
            @RequestParam @NonNull LocalDate toDate) {
        List<Vehicle> vehicles = vehicleService.getOurVehiclesByDateRange(fromDate, toDate);
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(vehicleMapper::vehicleToVehicleDetailsDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/all/by-date-range")
    public ResponseEntity<List<VehicleDetailsDTO>> getAllVehiclesByDateRange(
            @RequestParam @NonNull LocalDate fromDate,
            @RequestParam @NonNull LocalDate toDate) {
        List<Vehicle> vehicles = vehicleService.getAllVehiclesByDateRange(fromDate, toDate);
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(vehicleMapper::vehicleToVehicleDetailsDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('warehouse:edit') or hasAuthority('declarant:edit')")
    @PatchMapping("/{vehicleId}")
    public ResponseEntity<VehicleDetailsDTO> updateVehicle(
            @PathVariable @Positive Long vehicleId,
            @RequestBody @Valid @NonNull VehicleUpdateDTO dto) {
        Vehicle updated = vehicleService.updateVehicle(vehicleId, dto);
        return ResponseEntity.ok(vehicleMapper.vehicleToVehicleDetailsDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('warehouse:delete') or hasAuthority('declarant:delete')")
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable @Positive Long vehicleId) {
        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/cost")
    public ResponseEntity<Void> updateVehicleCost(
            @PathVariable @Positive Long vehicleId,
            @RequestBody @Valid @NonNull UpdateVehicleCostRequest request) {
        if ("add".equalsIgnoreCase(request.getOperation())) {
            vehicleService.addWithdrawalCost(vehicleId, request.getAmountEur());
        } else if ("subtract".equalsIgnoreCase(request.getOperation())) {
            vehicleService.subtractWithdrawalCost(vehicleId, request.getAmountEur());
        } else {
            throw new PurchaseException("INVALID_OPERATION",
                    String.format("Invalid operation: %s. Must be 'add' or 'subtract'", request.getOperation()));
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

        Map<String, List<String>> filterParams = parseFilters(filters);
        Page<Vehicle> vehiclePage = vehicleService.searchVehicles(query, pageable, filterParams);
        
        List<Vehicle> vehicles = vehiclePage.getContent();
        if (vehicles.isEmpty()) {
            return ResponseEntity.ok(new PageResponse<>(
                    vehiclePage.getNumber(),
                    vehiclePage.getSize(),
                    vehiclePage.getTotalElements(),
                    vehiclePage.getTotalPages(),
                    Collections.emptyList()
            ));
        }
        
        List<Long> vehicleIds = vehicles.stream()
                .map(Vehicle::getId)
                .toList();
        
        Map<Long, List<VehicleProduct>> vehicleProductsMap = vehicleService.getVehicleProductsByVehicleIds(vehicleIds);
        Map<Long, BigDecimal> expensesTotalMap = vehicleService.getExpensesTotalsByVehicleIds(vehicleIds);
        
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(vehicle -> {
                    BigDecimal expensesTotal = expensesTotalMap.getOrDefault(vehicle.getId(), BigDecimal.ZERO);
                    List<VehicleProduct> products = vehicleProductsMap.getOrDefault(vehicle.getId(), Collections.emptyList());
                    return vehicleMapper.vehicleToVehicleDetailsDTO(vehicle, products, expensesTotal);
                })
                .toList();
        
        return ResponseEntity.ok(new PageResponse<>(
                vehiclePage.getNumber(),
                vehiclePage.getSize(),
                vehiclePage.getTotalElements(),
                vehiclePage.getTotalPages(),
                dtos
        ));
    }

    private Map<String, List<String>> parseFilters(String filters) {
        try {
            if (filters != null && !filters.isEmpty()) {
                return objectMapper.readValue(filters, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, List.class));
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to parse filters: {}", filters, e);
            return Collections.emptyMap();
        }
    }
}

