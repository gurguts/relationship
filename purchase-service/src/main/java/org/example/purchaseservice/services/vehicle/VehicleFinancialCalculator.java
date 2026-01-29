package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleFinancialCalculator {
    
    private static final int RECLAMATION_SCALE = 6;
    
    public record VehicleProductsData(String productsText, BigDecimal totalCost) {}
    public record VehicleExpensesData(BigDecimal total) {}
    public record FinancialMetrics(BigDecimal totalExpenses, BigDecimal totalIncome, BigDecimal margin, ReclamationData reclamationData) {}
    public record ReclamationData(BigDecimal reclamationPerTon, BigDecimal fullReclamation) {}
    
    public VehicleProductsData calculateVehicleProductsData(@NonNull VehicleDetailsDTO vehicleDTO, 
                                                           @NonNull java.util.Map<Long, org.example.purchaseservice.models.Product> productMap,
                                                           @NonNull VehicleFieldValueFormatter formatter) {
        StringBuilder productsText = new StringBuilder();
        BigDecimal productsTotalCost = BigDecimal.ZERO;
        
        if (isNotEmpty(vehicleDTO.getItems())) {
            for (VehicleDetailsDTO.VehicleItemDTO item : vehicleDTO.getItems()) {
                if (!productsText.isEmpty()) {
                    productsText.append("\n");
                }
                String productName = formatter.getProductName(item.getProductId(), productMap);
                BigDecimal itemTotalCost = safeGetBigDecimal(item.getTotalCostEur());
                productsTotalCost = productsTotalCost.add(itemTotalCost);
                productsText.append(String.format("%s, Кількість: %s%s, Ціна: %s%s, Сума: %s%s",
                        productName,
                        formatter.formatNumber(item.getQuantity()),
                        VehicleFieldValueFormatter.KG_SUFFIX,
                        formatter.formatNumber(item.getUnitPriceEur()),
                        VehicleFieldValueFormatter.EUR_SUFFIX,
                        formatter.formatNumber(item.getTotalCostEur()),
                        VehicleFieldValueFormatter.EUR_SUFFIX));
            }
        }
        
        return new VehicleProductsData(productsText.toString(), productsTotalCost);
    }
    
    public VehicleExpensesData calculateVehicleExpensesData(@NonNull List<org.example.purchaseservice.models.balance.VehicleExpense> expenses) {
        BigDecimal expensesTotal = BigDecimal.ZERO;
        if (isNotEmpty(expenses)) {
            for (org.example.purchaseservice.models.balance.VehicleExpense expense : expenses) {
                BigDecimal convertedAmount = expense.getConvertedAmount();
                if (convertedAmount != null) {
                    expensesTotal = expensesTotal.add(convertedAmount);
                }
            }
        }
        return new VehicleExpensesData(expensesTotal);
    }
    
    public FinancialMetrics calculateFinancialMetrics(@NonNull VehicleProductsData productsData, 
                                                     @NonNull VehicleExpensesData expensesData,
                                                     @NonNull VehicleDetailsDTO vehicleDTO) {
        BigDecimal totalExpenses = productsData.totalCost().add(expensesData.total());
        BigDecimal invoiceEuTotalPrice = safeGetBigDecimal(vehicleDTO.getInvoiceEuTotalPrice());
        ReclamationData reclamationData = calculateReclamation(vehicleDTO);
        BigDecimal totalIncome = invoiceEuTotalPrice.subtract(reclamationData.fullReclamation());
        BigDecimal margin = totalIncome.subtract(totalExpenses);
        
        return new FinancialMetrics(totalExpenses, totalIncome, margin, reclamationData);
    }
    
    private ReclamationData calculateReclamation(@NonNull VehicleDetailsDTO vehicleDTO) {
        BigDecimal reclamationPerTon = safeGetBigDecimal(vehicleDTO.getReclamation());
        BigDecimal fullReclamation = BigDecimal.ZERO;
        
        if (reclamationPerTon.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal invoiceEuPricePerTon = safeGetBigDecimal(vehicleDTO.getInvoiceEuPricePerTon());
            if (invoiceEuPricePerTon.compareTo(BigDecimal.ZERO) > 0) {
                fullReclamation = reclamationPerTon.multiply(invoiceEuPricePerTon)
                        .setScale(RECLAMATION_SCALE, RoundingMode.HALF_UP);
            }
        }
        
        return new ReclamationData(reclamationPerTon, fullReclamation);
    }
    
    private BigDecimal safeGetBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    
    private boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
