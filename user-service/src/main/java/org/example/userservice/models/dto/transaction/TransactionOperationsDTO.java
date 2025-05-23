package org.example.userservice.models.dto.transaction;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionOperationsDTO {
    private Long targetUserId;
    private BigDecimal amount;
    private String currency;
    private String description;
}
