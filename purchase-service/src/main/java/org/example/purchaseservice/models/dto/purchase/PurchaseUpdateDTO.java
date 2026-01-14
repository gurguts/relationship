package org.example.purchaseservice.models.dto.purchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseUpdateDTO {

    private Long sourceId;

    private Long productId;

    @NotNull(message = "{validation.purchase.quantity.null}")
    @DecimalMin(value = "0.01", message = "{validation.purchase.quantity.zero}")
    private BigDecimal quantity;

    @NotNull(message = "{validation.purchase.totalPrice.null}")
    @DecimalMin(value = "0.00", message = "{validation.purchase.totalPrice.negative}")
    private BigDecimal totalPrice;

    @DecimalMin(value = "0.000001", message = "{validation.purchase.exchangeRate.invalid}")
    private BigDecimal exchangeRate;

    private String createdAt;

    private String comment;
}
