package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleExpenseUpdateDTO {
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    private Long fromAccountId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private String currency;
    
    private BigDecimal exchangeRate;
    
    private BigDecimal convertedAmount;
    
    private String description;
}

