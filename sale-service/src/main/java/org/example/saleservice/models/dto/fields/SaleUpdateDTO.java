package org.example.saleservice.models.dto.fields;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleUpdateDTO {
    private Long sourceId;

    @NotNull(message = "{validation.sale.product.null}")
    private Long productId;

    @NotNull(message = "{validation.sale.quantity.null}")
    @DecimalMin(value = "0.01", message = "{validation.sale.quantity.zero}")
    private BigDecimal quantity;

    @NotNull(message = "{validation.sale.totalPrice.null}")
    @DecimalMin(value = "0.00", message = "{validation.sale.totalPrice.negative}")
    private BigDecimal totalPrice;

    private String createdAt;
}
