package org.example.userservice.services.account;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.repositories.AccountBalanceRepository;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.services.impl.IAccountBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBalanceService implements IAccountBalanceService {
    private static final String ERROR_CODE_CURRENCY_NOT_SUPPORTED = "CURRENCY";

    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public void getBalance(@NonNull Long accountId, @NonNull String currency) {
        if (currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }
        String normalizedCurrency = currency.toUpperCase();
        accountBalanceRepository.findByAccountIdAndCurrency(accountId, normalizedCurrency)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account balance for account %d and currency %s not found", accountId, currency)));
    }

    @Override
    @Transactional
    public void addAmount(@NonNull Long accountId, @NonNull String currency, @NonNull BigDecimal amount) {
        log.info("Adding amount to account balance: accountId={}, currency={}, amount={}", accountId, currency, amount);
        validateAmount(amount);
        validateAccountBalance(accountId, currency);
        String normalizedCurrency = currency.toUpperCase();
        accountBalanceRepository.addAmount(accountId, normalizedCurrency, amount);
        log.info("Amount added to account balance: accountId={}, currency={}, amount={}", accountId, normalizedCurrency, amount);
    }

    @Override
    @Transactional
    public void subtractAmount(@NonNull Long accountId, @NonNull String currency, @NonNull BigDecimal amount) {
        log.info("Subtracting amount from account balance: accountId={}, currency={}, amount={}", accountId, currency, amount);
        validateAmount(amount);
        validateAccountBalance(accountId, currency);
        String normalizedCurrency = currency.toUpperCase();
        accountBalanceRepository.subtractAmount(accountId, normalizedCurrency, amount);
        log.info("Amount subtracted from account balance: accountId={}, currency={}, amount={}", accountId, normalizedCurrency, amount);
    }

    private void validateAccountBalance(@NonNull Long accountId, @NonNull String currency) {
        if (currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }
        String normalizedCurrency = currency.toUpperCase();
        accountBalanceRepository
                .findByAccountIdAndCurrency(accountId, normalizedCurrency)
                .orElseThrow(() -> {
                    if (!accountRepository.existsById(accountId)) {
                        return new AccountNotFoundException(
                                String.format("Account with ID %d not found", accountId));
                    }
                    return new AccountException(ERROR_CODE_CURRENCY_NOT_SUPPORTED,
                            String.format("Currency %s is not supported for account %d", currency, accountId));
                });
    }

    private void validateAmount(@NonNull BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
