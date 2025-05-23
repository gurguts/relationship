package org.example.transactionservice.models.dto;

import lombok.Data;
import org.example.transactionservice.models.TransactionType;

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