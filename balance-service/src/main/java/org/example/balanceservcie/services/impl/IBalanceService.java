package org.example.balanceservcie.services.impl;

import org.example.balanceservcie.models.Balance;

import java.math.BigDecimal;

public interface IBalanceService {
    BigDecimal getBalanceValue(Long balanceId, String currency);

    void updateBalance(Long balanceId, BigDecimal balanceDifference, String currency);

    BigDecimal getUserBalance(Long userId, String currency);

    void updateUserBalance(Long userId, BigDecimal balanceDifference, String currency);

    Long createUserBalance(Long userId);

    void deleteBalanceUser(Long userId);

    Balance getBalanceByUserId(Long userId);
}
