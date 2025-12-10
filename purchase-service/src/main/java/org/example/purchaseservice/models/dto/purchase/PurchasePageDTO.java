package org.example.purchaseservice.models.dto.purchase;

import lombok.Data;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.dto.client.ClientDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchasePageDTO {
    private Long id;

    private Long userId;

    private ClientDTO client;

    private Long sourceId;

    private Long productId;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private BigDecimal totalPriceEur;

    private PaymentMethod paymentMethod;

    private String currency;

    private BigDecimal exchangeRate;

    private Long transactionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String comment;

    private Boolean isReceived;
}
