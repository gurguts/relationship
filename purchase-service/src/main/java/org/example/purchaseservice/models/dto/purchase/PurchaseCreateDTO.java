package org.example.purchaseservice.models.dto.purchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.purchaseservice.models.PaymentMethod;

import java.math.BigDecimal;

@Data
public class PurchaseCreateDTO {
    @NotNull(message = "{validation.purchase.client.null}")
    private Long clientId;

    private Long sourceId;

    @NotNull(message = "{validation.purchase.product.null}")
    private Long productId;

    @NotNull(message = "{validation.purchase.quantity.null}")
    @DecimalMin(value = "0.01", message = "{validation.purchase.quantity.zero}")
    private BigDecimal quantity;

    @NotNull(message = "{validation.purchase.totalPrice.null}")
    @DecimalMin(value = "0.00", message = "{validation.purchase.totalPrice.negative}")
    private BigDecimal totalPrice;

    @NotNull(message = "{validation.purchase.paymentMethod.null}")
    private PaymentMethod paymentMethod;

    @NotNull(message = "{validation.purchase.currency.null}")
    private String currency;
}
