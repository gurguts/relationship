package org.example.userservice.models.dto.transaction;

import lombok.Data;
import org.example.userservice.models.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDateTime createdAt;
    private Long client;
    private Long executorUserId;
    private String currency;
}