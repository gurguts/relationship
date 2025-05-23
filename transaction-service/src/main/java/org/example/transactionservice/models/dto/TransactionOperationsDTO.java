package org.example.transactionservice.models.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionOperationsDTO {
    private Long targetUserId;
    private BigDecimal amount;
    private String description;
    private String currency;
}
