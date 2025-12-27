package org.example.purchaseservice.models.dto.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleExpenseCreateDTO {
    private Long vehicleId;
    private Long fromAccountId;
    private Long categoryId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private String description;
}

