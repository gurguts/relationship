package org.example.balanceservcie.models.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceUpdateDTO {
    private BigDecimal balanceDifference;
    private String currency;
}
