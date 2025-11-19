package org.example.userservice.models.dto.transaction;

import lombok.Data;
import org.example.userservice.models.transaction.TransactionType;

import java.math.BigDecimal;

@Data
public class TransactionCreateRequestDTO {
    private TransactionType type;
    private Long categoryId;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String currency;
    private String convertedCurrency;
    private BigDecimal exchangeRate;
    private Long clientId;
    private String description;
}

