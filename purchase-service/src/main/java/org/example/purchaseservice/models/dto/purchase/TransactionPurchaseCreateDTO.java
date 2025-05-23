package org.example.purchaseservice.models.dto.purchase;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionPurchaseCreateDTO {
    private Long targetUserId;
    private Long executorUserId;
    private Long clientId;
    private BigDecimal totalPrice;
    private Long productId;
    private String currency;
}
