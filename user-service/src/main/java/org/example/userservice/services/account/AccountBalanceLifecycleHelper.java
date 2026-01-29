package org.example.userservice.services.account;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.repositories.AccountBalanceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccountBalanceLifecycleHelper {

    private static final String ERROR_CODE_CURRENCY_NOT_REMOVABLE = "CURRENCY";
    private static final String ERROR_CODE_BALANCE_NOT_ZERO = "BALANCE";

    private final AccountBalanceRepository accountBalanceRepository;

    public void createBalancesForAccount(@NonNull Long accountId, @NonNull Set<String> currencies) {
        List<AccountBalance> balances = currencies.stream()
                .map(currency -> createAccountBalance(accountId, currency))
                .toList();
        accountBalanceRepository.saveAll(balances);
    }

    public void syncBalancesOnUpdate(@NonNull Long accountId, @NonNull Set<String> oldCurrencies, @NonNull Set<String> newCurrencies) {
        createNewBalancesForAccount(accountId, newCurrencies, oldCurrencies);
        removeOldBalancesForAccount(accountId, oldCurrencies, newCurrencies);
    }

    public void validateAccountCanBeDeleted(@NonNull Long accountId) {
        long nonZeroCount = accountBalanceRepository.countNonZeroBalances(accountId);
        if (nonZeroCount > 0) {
            throw new AccountException(ERROR_CODE_BALANCE_NOT_ZERO,
                    String.format("Cannot delete account: has %d non-zero balance(s)", nonZeroCount));
        }
    }

    private void createNewBalancesForAccount(Long accountId, Set<String> newCurrencies, Set<String> oldCurrencies) {
        List<AccountBalance> balancesToCreate = newCurrencies.stream()
                .filter(currency -> !oldCurrencies.contains(currency))
                .map(currency -> createAccountBalance(accountId, currency))
                .toList();

        if (!balancesToCreate.isEmpty()) {
            accountBalanceRepository.saveAll(balancesToCreate);
        }
    }

    private Map<String, AccountBalance> buildBalanceMap(Long accountId) {
        List<AccountBalance> allBalances = accountBalanceRepository.findByAccountId(accountId);
        return allBalances.stream()
                .collect(Collectors.toMap(
                        AccountBalance::getCurrency,
                        balance -> balance,
                        (existing, _) -> existing
                ));
    }

    private void removeOldBalancesForAccount(Long accountId, Set<String> oldCurrencies, Set<String> newCurrencies) {
        Map<String, AccountBalance> balanceMap = buildBalanceMap(accountId);

        List<AccountBalance> balancesToDelete = oldCurrencies.stream()
                .filter(currency -> !newCurrencies.contains(currency))
                .map(balanceMap::get)
                .filter(balance -> {
                    if (balance != null && balance.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                        throw new AccountException(ERROR_CODE_CURRENCY_NOT_REMOVABLE,
                                String.format("Cannot remove currency %s: account has non-zero balance", balance.getCurrency()));
                    }
                    return balance != null;
                })
                .toList();

        if (!balancesToDelete.isEmpty()) {
            accountBalanceRepository.deleteAll(balancesToDelete);
        }
    }

    private AccountBalance createAccountBalance(Long accountId, String currency) {
        if (currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }
        AccountBalance balance = new AccountBalance();
        balance.setAccountId(accountId);
        balance.setCurrency(currency.toUpperCase());
        balance.setAmount(BigDecimal.ZERO);
        return balance;
    }
}
