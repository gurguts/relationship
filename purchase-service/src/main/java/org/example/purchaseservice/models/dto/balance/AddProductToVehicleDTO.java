package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddProductToVehicleDTO {
    
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
}

