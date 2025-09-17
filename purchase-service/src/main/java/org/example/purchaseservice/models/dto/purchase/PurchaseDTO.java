package org.example.purchaseservice.models.dto.purchase;

import lombok.Data;
import org.example.purchaseservice.models.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseDTO {
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

    private BigDecimal exchangeRate;

    private Long transactionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String comment;
}
