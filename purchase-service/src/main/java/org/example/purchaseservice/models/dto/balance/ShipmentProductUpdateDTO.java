package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShipmentProductUpdateDTO {
    @DecimalMin(value = "0.00", inclusive = true, message = "Quantity cannot be negative")
    @Digits(integer = 14, fraction = 2)
    private BigDecimal quantity;

    @DecimalMin(value = "0.000001", inclusive = true, message = "Total cost must be greater than zero")
    @Digits(integer = 14, fraction = 6)
    private BigDecimal totalCostUah;
}
