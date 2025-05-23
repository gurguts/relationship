package org.example.saleservice.models.dto.fields;

import lombok.Data;
import org.example.saleservice.models.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SaleDTO {
    private Long id;

    private Long userId;

    private Long clientId;

    private Long sourceId;

    private Long productId;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private PaymentMethod paymentMethod;

    private String currency;

    private Long transactionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
