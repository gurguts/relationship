package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IVehicleService {
    
    Vehicle createVehicle(@NonNull Vehicle vehicle);
    
    void addWithdrawalCost(@NonNull Long vehicleId, BigDecimal withdrawalCost);
    
    void subtractWithdrawalCost(@NonNull Long vehicleId, BigDecimal withdrawalCost);
    
    Vehicle getVehicle(@NonNull Long vehicleId);
    
    List<Vehicle> getVehiclesByIds(@NonNull List<Long> ids);
    
    List<Vehicle> getVehiclesByDate(@NonNull LocalDate date);
    
    List<Vehicle> getOurVehiclesByDateRange(@NonNull LocalDate fromDate, @NonNull LocalDate toDate);
    
    Page<Vehicle> findOurVehiclesPaged(String query, LocalDate fromDate, LocalDate toDate, List<Long> managerIds, @NonNull Pageable pageable);
    
    List<Vehicle> getAllVehiclesByDateRange(@NonNull LocalDate fromDate, @NonNull LocalDate toDate);
    
    Vehicle updateVehicleProduct(@NonNull Long vehicleId, @NonNull Long vehicleProductId,
                                  BigDecimal newQuantity, BigDecimal newTotalCost);
    
    Vehicle updateVehicle(@NonNull Long vehicleId, @NonNull VehicleUpdateDTO dto);
    
    Vehicle addProductToVehicle(@NonNull Long vehicleId, @NonNull Long warehouseId, 
                                @NonNull Long productId, @NonNull BigDecimal quantity, Long userId);
    
    List<VehicleProduct> getVehicleProducts(@NonNull Long vehicleId);
    
    void deleteVehicle(@NonNull Long vehicleId);
    
    Page<Vehicle> searchVehicles(String query, @NonNull Pageable pageable, Map<String, List<String>> filterParams);

    BigDecimal calculateTotalExpenses(@NonNull List<VehicleProduct> products, @NonNull BigDecimal expensesTotal);
    
    BigDecimal calculateTotalIncome(@NonNull Vehicle vehicle);

    BigDecimal calculateMargin(@NonNull Vehicle vehicle, @NonNull List<VehicleProduct> products, @NonNull BigDecimal expensesTotal);
    
    Map<Long, List<VehicleProduct>> getVehicleProductsByVehicleIds(@NonNull List<Long> vehicleIds);
    
    Map<Long, BigDecimal> getExpensesTotalsByVehicleIds(@NonNull List<Long> vehicleIds);
}

