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
import org.example.purchaseservice.models.dto.balance.AddProductToVehicleDTO;
import org.example.purchaseservice.models.dto.balance.UpdateVehicleCostRequest;
import org.example.purchaseservice.models.dto.balance.VehicleCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseUpdateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleProductUpdateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.mappers.VehicleMapper;
import org.example.purchaseservice.services.balance.VehicleService;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
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
    private final org.example.purchaseservice.services.balance.VehicleExpenseService vehicleExpenseService;
    private final VehicleMapper vehicleMapper;
    private final VehicleProductRepository vehicleProductRepository;
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleDetailsDTO> createVehicle(@RequestBody @Valid @NonNull VehicleCreateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
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

            byte[] excelData = vehicleExportService.exportToExcel(query, filterParams);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=vehicles.xlsx")
                    .body(excelData);
        } catch (IOException e) {
            log.error("Error exporting vehicles to Excel", e);
            return ResponseEntity.internalServerError().build();
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
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/products")
    public ResponseEntity<VehicleDetailsDTO> addProductToVehicle(
            @PathVariable @Positive Long vehicleId,
            @RequestBody @Valid @NonNull AddProductToVehicleDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
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
        
        Map<Long, List<VehicleProduct>> vehicleProductsMap = vehicleProductRepository.findByVehicleIdIn(vehicleIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(VehicleProduct::getVehicleId));
        
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap = 
                vehicleExpenseService.getExpensesByVehicleIds(vehicleIds);
        
        Map<Long, BigDecimal> expensesTotalMap = expensesMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(org.example.purchaseservice.models.balance.VehicleExpense::getConvertedAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                ));
        
        List<VehicleDetailsDTO> dtos = vehicles.stream()
                .map(vehicle -> {
                    BigDecimal expensesTotal = expensesTotalMap.getOrDefault(vehicle.getId(), java.math.BigDecimal.ZERO);
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

    @PreAuthorize("hasAuthority('declarant:create')")
    @PostMapping("/{vehicleId}/expenses")
    public ResponseEntity<VehicleExpense> createVehicleExpense(
            @PathVariable @Positive Long vehicleId,
            @RequestBody @Valid @NonNull VehicleExpenseCreateDTO dto) {
        VehicleExpenseCreateDTO dtoWithVehicleId = new VehicleExpenseCreateDTO();
        BeanUtils.copyProperties(dto, dtoWithVehicleId);
        dtoWithVehicleId.setVehicleId(vehicleId);
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

