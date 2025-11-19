package org.example.purchaseservice.models.dto.transaction;

import lombok.Data;
import org.example.purchaseservice.models.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private Long targetUserId;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private TransactionType type;
    private Long categoryId;
    private String description;
    private LocalDateTime createdAt;
    private Long clientId;
    private Long executorUserId;
    private String currency;
    private BigDecimal exchangeRate;
    private String convertedCurrency;
    private BigDecimal convertedAmount;
}

