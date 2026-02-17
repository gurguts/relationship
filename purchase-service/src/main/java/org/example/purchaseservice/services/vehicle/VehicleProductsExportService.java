package org.example.purchaseservice.services.vehicle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.services.impl.IVehicleProductsExportService;
import org.example.purchaseservice.services.impl.IVehicleService;
import org.example.purchaseservice.services.impl.IUserService;
import org.example.purchaseservice.spec.StockVehicleSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleProductsExportService implements IVehicleProductsExportService {

    private final VehicleRepository vehicleRepository;
    private final IVehicleService vehicleService;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final IUserService userService;
    private final VehicleProductsExcelGenerator excelGenerator;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportVehicleProductsToExcel(String query, LocalDate fromDate, LocalDate toDate, List<Long> managerIds) throws IOException {
        StockVehicleSpecification spec = new StockVehicleSpecification(query, fromDate, toDate, managerIds != null ? managerIds : List.of());
        Sort sort = Sort.by(Sort.Direction.DESC, "shipmentDate");
        List<Vehicle> vehicles = vehicleRepository.findAll(spec, sort);

        if (vehicles.isEmpty()) {
            return excelGenerator.createEmptyWorkbook();
        }

        List<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();
        Map<Long, List<VehicleProduct>> productsByVehicle = vehicleService.getVehicleProductsByVehicleIds(vehicleIds);
        Map<Long, Vehicle> vehicleMap = vehicles.stream().collect(Collectors.toMap(Vehicle::getId, v -> v));

        List<Long> warehouseIds = productsByVehicle.values().stream()
                .flatMap(List::stream)
                .map(VehicleProduct::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> productIds = productsByVehicle.values().stream()
                .flatMap(List::stream)
                .map(VehicleProduct::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> warehouseNameMap = loadWarehouseNames(warehouseIds);
        Map<Long, String> productNameMap = loadProductNames(productIds);
        Map<Long, String> managerNameMap = loadManagerNames(vehicles);

        List<VehicleProductsExcelGenerator.VehicleProductExcelRow> rows = new ArrayList<>();
        for (Long vehicleId : vehicleIds) {
            Vehicle vehicle = vehicleMap.get(vehicleId);
            if (vehicle == null) continue;
            List<VehicleProduct> products = productsByVehicle.getOrDefault(vehicleId, Collections.emptyList());
            String vehicleNumber = vehicle.getVehicleNumber() != null ? vehicle.getVehicleNumber() : "";
            LocalDate withdrawalDate = vehicle.getShipmentDate();
            Long id = vehicle.getId();
            String vehicleComment = vehicle.getDescription() != null ? vehicle.getDescription() : "";
            String managerName = vehicle.getManagerId() != null ? managerNameMap.getOrDefault(vehicle.getManagerId(), "") : "";
            for (VehicleProduct vp : products) {
                String warehouseName = warehouseNameMap.getOrDefault(vp.getWarehouseId(), "");
                String productName = productNameMap.getOrDefault(vp.getProductId(), "");
                BigDecimal pricePerKg = vp.getUnitPriceEur() != null ? vp.getUnitPriceEur() : BigDecimal.ZERO;
                BigDecimal totalCost = vp.getTotalCostEur() != null ? vp.getTotalCostEur() : BigDecimal.ZERO;
                BigDecimal quantity = vp.getQuantity() != null ? vp.getQuantity() : BigDecimal.ZERO;
                rows.add(new VehicleProductsExcelGenerator.VehicleProductExcelRow(
                        id,
                        vehicleNumber,
                        warehouseName,
                        productName,
                        quantity,
                        pricePerKg,
                        totalCost,
                        withdrawalDate,
                        managerName,
                        vehicleComment
                ));
            }
        }

        return excelGenerator.generate(rows);
    }

    private Map<Long, String> loadWarehouseNames(List<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) return Collections.emptyMap();
        return warehouseRepository.findAllById(warehouseIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Warehouse::getId, w -> w.getName() != null ? w.getName() : ""));
    }

    private Map<Long, String> loadProductNames(List<Long> productIds) {
        if (productIds.isEmpty()) return Collections.emptyMap();
        Map<Long, String> map = new HashMap<>();
        for (Product p : productRepository.findAllById(productIds)) {
            if (p != null && p.getId() != null) {
                map.put(p.getId(), p.getName() != null ? p.getName() : "");
            }
        }
        return map;
    }

    private Map<Long, String> loadManagerNames(List<Vehicle> vehicles) {
        Set<Long> managerIds = vehicles.stream()
                .map(Vehicle::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (managerIds.isEmpty()) return Collections.emptyMap();
        try {
            List<UserDTO> users = userService.getAllUsers();
            if (users == null) return Collections.emptyMap();
            Map<Long, String> map = new HashMap<>();
            for (UserDTO user : users) {
                if (user != null && user.getId() != null && managerIds.contains(user.getId())) {
                    map.put(user.getId(), user.getName() != null ? user.getName() : "");
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to load manager names for export", e);
            return Collections.emptyMap();
        }
    }
}
