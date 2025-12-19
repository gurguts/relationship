package org.example.userservice.services.account;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.repositories.AccountBalanceRepository;
import org.example.userservice.repositories.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountBalanceService {
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public AccountBalance getBalance(Long accountId, String currency) {
        return accountBalanceRepository.findByAccountIdAndCurrency(accountId, currency.toUpperCase())
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account balance for account %d and currency %s not found", accountId, currency)));
    }

    @Transactional
    public void addAmount(Long accountId, String currency, BigDecimal amount) {
        accountBalanceRepository
                .findByAccountIdAndCurrency(accountId, currency.toUpperCase())
                .orElseThrow(() -> {
                    if (!accountRepository.existsById(accountId)) {
                        return new AccountNotFoundException(
                                String.format("Account with ID %d not found", accountId));
                    }
                    return new AccountException("CURRENCY",
                            String.format("Currency %s is not supported for account %d", currency, accountId));
                });

        accountBalanceRepository.addAmount(accountId, currency.toUpperCase(), amount);
    }

    @Transactional
    public void subtractAmount(Long accountId, String currency, BigDecimal amount) {
        accountBalanceRepository
                .findByAccountIdAndCurrency(accountId, currency.toUpperCase())
                .orElseThrow(() -> {
                    if (!accountRepository.existsById(accountId)) {
                        return new AccountNotFoundException(
                                String.format("Account with ID %d not found", accountId));
                    }
                    return new AccountException("CURRENCY",
                            String.format("Currency %s is not supported for account %d", currency, accountId));
                });

        accountBalanceRepository.subtractAmount(accountId, currency.toUpperCase(), amount);
    }

    @Transactional
    public void transferAmount(Long fromAccountId, String fromCurrency, Long toAccountId, String toCurrency, BigDecimal amount) {
        subtractAmount(fromAccountId, fromCurrency, amount);
        addAmount(toAccountId, toCurrency, amount);
    }
}

