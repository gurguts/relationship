package org.example.userservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.balance.BalanceException;
import org.example.userservice.exceptions.balance.BalanceNotFoundException;
import org.example.userservice.models.balance.Balance;
import org.example.userservice.repositories.BalanceRepository;
import org.example.userservice.services.impl.IBalanceService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceService implements IBalanceService {
    private final BalanceRepository balanceRepository;

    @Override
    public BigDecimal getBalanceValue(Long balanceId, String currency) {
        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new BalanceNotFoundException(
                        String.format("Balance not found with id: %d", balanceId)));

        return getBalanceByCurrency(balance, currency);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"usersBalance"}, allEntries = true)
    public void updateBalance(Long balanceId, BigDecimal balanceDifference, String currency) {
        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new BalanceNotFoundException(
                        String.format("Balance with ID %d not found", balanceId)));

        updateBalanceByCurrency(balance, balanceDifference, currency);
        balanceRepository.save(balance);
    }

    @Override
    @CacheEvict(value = {"usersBalance"}, allEntries = true)
    public void updateUserBalance(Long userId, BigDecimal balanceDifference, String currency) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceNotFoundException(
                        String.format("Balance for user with ID %d not found", userId)));

        updateBalanceByCurrency(balance, balanceDifference, currency);
        balanceRepository.save(balance);
    }

    @Override
    public void createUserBalance(Long userId) {
        Balance balance = new Balance();
        balance.setUserId(userId);
        balanceRepository.save(balance);
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
                .orElseThrow(() -> new BalanceNotFoundException(
                        String.format("Balance for user with ID %d not found", userId)));
    }

    @Override
    public List<Balance> findByUserIdIn(List<Long> userIds) {
        return balanceRepository.findByUserIdIn(userIds);
    }

    private BigDecimal getBalanceByCurrency(Balance balance, String currency) {
        return switch (currency.toUpperCase()) {
            case "UAH" -> balance.getBalanceUAH();
            case "EUR" -> balance.getBalanceEUR();
            case "USD" -> balance.getBalanceUSD();
            default -> throw new BalanceException("CURRENCY", String.format("Unsupported currency: %s", currency));
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
                throw new BalanceException("CURRENCY", String.format("Unsupported currency: %s", currency));
        }
    }
}
