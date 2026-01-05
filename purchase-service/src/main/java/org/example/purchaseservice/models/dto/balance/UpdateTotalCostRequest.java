package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateTotalCostRequest {
    @NotNull(message = "Total cost EUR is required")
    @DecimalMin(value = "0.00", message = "Total cost must be non-negative")
    private BigDecimal totalCostEur;
}
