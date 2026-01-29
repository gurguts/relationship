package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.services.impl.IVehicleExpenseService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleCostCalculator {
    
    private final IVehicleExpenseService vehicleExpenseService;
    
    private static final int PRICE_SCALE = 6;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final RoundingMode UNIT_PRICE_ROUNDING_MODE = RoundingMode.CEILING;
    
    public BigDecimal calculateTotalCost(BigDecimal quantity, BigDecimal averagePrice) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity must be positive");
        }
        if (averagePrice == null) {
            throw new PurchaseException("INVALID_AVERAGE_PRICE", "Average price cannot be null");
        }
        return quantity.multiply(averagePrice).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    public BigDecimal calculateUnitPrice(BigDecimal totalCost, BigDecimal quantity) {
        if (totalCost == null) {
            throw new PurchaseException("INVALID_TOTAL_COST", "Total cost cannot be null");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Cannot calculate unit price: quantity is zero or null");
        }
        return totalCost.divide(quantity, PRICE_SCALE, UNIT_PRICE_ROUNDING_MODE);
    }
    
    public BigDecimal calculateTotalExpenses(@NonNull List<VehicleProduct> products, @NonNull BigDecimal expensesTotal) {
        BigDecimal totalCostEur = products.stream()
                .map(VehicleProduct::getTotalCostEur)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalCostEur.add(expensesTotal);
    }
    
    public BigDecimal calculateTotalIncome(@NonNull Vehicle vehicle) {
        BigDecimal invoiceEuTotalPrice = vehicle.getInvoiceEuTotalPrice() != null 
                ? vehicle.getInvoiceEuTotalPrice() 
                : BigDecimal.ZERO;
        BigDecimal fullReclamation = calculateFullReclamation(vehicle);
        return invoiceEuTotalPrice.subtract(fullReclamation);
    }
    
    public BigDecimal calculateFullReclamation(@NonNull Vehicle vehicle) {
        BigDecimal reclamationPerTon = vehicle.getReclamation() != null 
                ? vehicle.getReclamation() 
                : BigDecimal.ZERO;
        if (reclamationPerTon.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal invoiceEuPricePerTon = vehicle.getInvoiceEuPricePerTon() != null 
                ? vehicle.getInvoiceEuPricePerTon() 
                : BigDecimal.ZERO;
        if (invoiceEuPricePerTon.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return reclamationPerTon.multiply(invoiceEuPricePerTon).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }
    
    public BigDecimal calculateMargin(@NonNull Vehicle vehicle, @NonNull List<VehicleProduct> products, 
                                     @NonNull BigDecimal expensesTotal) {
        BigDecimal totalExpenses = calculateTotalExpenses(products, expensesTotal);
        BigDecimal totalIncome = calculateTotalIncome(vehicle);
        return totalIncome.subtract(totalExpenses);
    }
    
    public Map<Long, BigDecimal> getExpensesTotalsByVehicleIds(@NonNull List<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap = 
                vehicleExpenseService.getExpensesByVehicleIds(vehicleIds);
        return expensesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(org.example.purchaseservice.models.balance.VehicleExpense::getConvertedAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ));
    }
    
    public void addVehicleTotalCost(Vehicle vehicle, BigDecimal cost) {
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = vehicle.getTotalCostEur();
            if (currentTotalCost == null) {
                currentTotalCost = BigDecimal.ZERO;
            }
            vehicle.setTotalCostEur(currentTotalCost.add(cost));
        }
    }
    
    public void subtractVehicleTotalCost(Vehicle vehicle, BigDecimal cost) {
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotalCost = vehicle.getTotalCostEur();
            if (currentTotalCost == null) {
                currentTotalCost = BigDecimal.ZERO;
            }
            BigDecimal newTotalCost = currentTotalCost.subtract(cost);
            if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
                newTotalCost = BigDecimal.ZERO;
            }
            vehicle.setTotalCostEur(newTotalCost);
        }
    }
}
