package org.example.saleservice.models.dto.fields;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.example.saleservice.models.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SaleModalDTO {
    private Long id;

    private Long userId;

    private Long sourceId;

    private Long productId;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private PaymentMethod paymentMethod;

    private String currency;

    private BigDecimal exchangeRate;

    private LocalDateTime createdAt;
}
