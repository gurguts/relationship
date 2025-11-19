package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitialWarehouseBalanceDTO {
    
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Initial quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be positive")
    private BigDecimal initialQuantity;
    
    @NotNull(message = "Average price is required")
    @DecimalMin(value = "0.00", message = "Average price must be non-negative")
    private BigDecimal averagePriceUah;
    
    private String note;
}

