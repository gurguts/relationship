package org.example.purchaseservice.models.dto.purchase;

import lombok.Data;
import org.example.purchaseservice.models.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseModalDTO {
    private Long id;

    private Long userId;

    private Long sourceId;

    private Long productId;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private PaymentMethod paymentMethod;

    private String currency;

    private LocalDateTime createdAt;
}
