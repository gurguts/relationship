package org.example.userservice.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateVehicleCostRequest {
    @NotNull(message = "Amount EUR is required")
    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    private BigDecimal amountEur;

    @NotBlank(message = "Operation is required")
    @Pattern(regexp = "^(?i)(add|subtract)$", message = "Operation must be 'add' or 'subtract'")
    private String operation;
}

