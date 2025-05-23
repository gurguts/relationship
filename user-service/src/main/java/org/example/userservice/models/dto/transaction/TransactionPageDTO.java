package org.example.userservice.models.dto.transaction;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionPageDTO {
    private Long targetUserId;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDateTime createdAt;
    private String clientCompany;
    private String currency;
    private Long executorUserId;
}
