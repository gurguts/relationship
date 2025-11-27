package org.example.userservice.models.dto.transaction;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionUpdateDTO {
    private Long categoryId;
    private String description;
    private BigDecimal amount;
    private BigDecimal exchangeRate;
    private BigDecimal commission;
}

