package org.example.userservice.models.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceDTO {
    private Long id;
    private Long accountId;
    private String currency;
    private BigDecimal amount;
    private LocalDateTime updatedAt;
}

