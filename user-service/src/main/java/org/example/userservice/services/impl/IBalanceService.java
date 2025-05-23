package org.example.userservice.services.impl;

import org.example.userservice.models.balance.Balance;

import java.math.BigDecimal;
import java.util.List;

public interface IBalanceService {
    BigDecimal getBalanceValue(Long balanceId, String currency);

    void updateBalance(Long balanceId, BigDecimal balanceDifference, String currency);

    void updateUserBalance(Long userId, BigDecimal balanceDifference, String currency);

    void createUserBalance(Long userId);

    void deleteBalanceUser(Long userId);

    Balance getBalanceByUserId(Long userId);

    List<Balance> findByUserIdIn(List<Long> userIds);
}
