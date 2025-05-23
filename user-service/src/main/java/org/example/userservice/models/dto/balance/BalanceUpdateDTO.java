package org.example.userservice.models.dto.balance;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BalanceUpdateDTO {
    private BigDecimal balanceDifference;
    private String currency;
}
