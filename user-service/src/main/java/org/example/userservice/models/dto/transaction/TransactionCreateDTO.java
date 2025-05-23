package org.example.userservice.models.dto.transaction;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionCreateDTO {
    private Long targetUserId;
    private Long executorUserId;
    private Long clientId;
    private BigDecimal totalPrice;
    private Long productId;
    private String currency;
}