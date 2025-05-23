package org.example.balanceservcie.services;

import lombok.RequiredArgsConstructor;
import org.example.balanceservcie.exception.BalanceException;
import org.example.balanceservcie.exception.BalanceNotFoundException;
import org.example.balanceservcie.repositories.BalanceRepository;
import org.example.balanceservcie.services.impl.IBalanceService;
import org.springframework.stereotype.Service;
import org.example.balanceservcie.models.Balance;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceService implements IBalanceService {
    private final BalanceRepository balanceRepository;

    @Override
    public BigDecimal getBalanceValue(Long balanceId, String currency) {
        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new BalanceNotFoundException("Balance not found with id: " + balanceId));

        return getBalanceByCurrency(balance, currency);
    }

    @Override
    @Transactional
    public void updateBalance(Long balanceId, BigDecimal balanceDifference, String currency) {
        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new BalanceNotFoundException("Balance with ID " + balanceId + " not found"));

        updateBalanceByCurrency(balance, balanceDifference, currency);
        balanceRepository.save(balance);
    }

    @Override
    public BigDecimal getUserBalance(Long userId, String currency) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceNotFoundException("Balance for user with ID " + userId + " not found"));

        return getBalanceByCurrency(balance, currency);
    }

    @Override
    public void updateUserBalance(Long userId, BigDecimal balanceDifference, String currency) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceNotFoundException("Balance for user with ID " + userId + " not found"));

        updateBalanceByCurrency(balance, balanceDifference, currency);
        balanceRepository.save(balance);
    }

    @Override
    public Long createUserBalance(Long userId) {
        Balance balance = new Balance();
        balance.setUserId(userId);
        return balanceRepository.save(balance).getId();
    }

    @Override
    @Transactional
    public void deleteBalanceUser(Long userId) {
        if (userId == null) {
            throw new BalanceException("USER", "User ID cannot be null");
        }
        balanceRepository.deleteByUserId(userId);
    }

    @Override
    public Balance getBalanceByUserId(Long userId) {
        return balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceNotFoundException("Balance for user with ID " + userId + " not found"));
    }

    private BigDecimal getBalanceByCurrency(Balance balance, String currency) {
        return switch (currency.toUpperCase()) {
            case "UAH" -> balance.getBalanceUAH();
            case "EUR" -> balance.getBalanceEUR();
            case "USD" -> balance.getBalanceUSD();
            default -> throw new BalanceException("CURRENCY", "Unsupported currency: " + currency);
        };
    }

    private void updateBalanceByCurrency(Balance balance, BigDecimal difference, String currency) {
        switch (currency.toUpperCase()) {
            case "UAH":
                balance.setBalanceUAH(balance.getBalanceUAH().add(difference));
                break;
            case "EUR":
                balance.setBalanceEUR(balance.getBalanceEUR().add(difference));
                break;
            case "USD":
                balance.setBalanceUSD(balance.getBalanceUSD().add(difference));
                break;
            default:
                throw new BalanceException("CURRENCY", "Unsupported currency: " + currency);
        }
    }
}
