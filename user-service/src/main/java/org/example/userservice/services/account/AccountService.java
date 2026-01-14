package org.example.userservice.services.account;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.repositories.AccountBalanceRepository;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.services.impl.IAccountService;
import org.example.userservice.services.impl.IBranchPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements IAccountService {
    private static final String ERROR_CODE_CURRENCIES_REQUIRED = "CURRENCIES";
    private static final String ERROR_CODE_CURRENCY_NOT_REMOVABLE = "CURRENCY";
    private static final String ERROR_CODE_BALANCE_NOT_ZERO = "BALANCE";

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final IBranchPermissionService branchPermissionService;

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsAccessibleToUser(@NonNull Long userId) {
        List<Account> allAccounts = accountRepository.findAllByOrderByNameAsc();
        Set<Long> accessibleBranchIds = getAccessibleBranchIds(userId);
        
        return allAccounts.stream()
                .filter(account -> {
                    if (account.getBranchId() == null) {
                        return true;
                    }
                    return accessibleBranchIds.contains(account.getBranchId());
                })
                .toList();
    }

    private Set<Long> getAccessibleBranchIds(@NonNull Long userId) {
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(userId);
        return userPermissions.stream()
                .filter(org.example.userservice.models.branch.BranchPermission::getCanView)
                .map(org.example.userservice.models.branch.BranchPermission::getBranchId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(@NonNull Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsByBranchId(@NonNull Long branchId) {
        return accountRepository.findByBranchId(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountById(@NonNull Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account with ID %d not found", id)));
    }

    @Override
    @Transactional
    public Account createAccount(@NonNull Account account) {
        log.info("Creating account: name={}", account.getName());
        validateCurrencies(account.getCurrencies());

        Account savedAccount = accountRepository.save(account);
        createBalancesForAccount(savedAccount.getId(), account.getCurrencies());

        log.info("Account created: id={}", savedAccount.getId());
        return savedAccount;
    }

    @Override
    @Transactional
    public Account updateAccount(@NonNull Long id, @NonNull Account updatedAccount) {
        log.info("Updating account: id={}", id);
        Account account = getAccountById(id);
        updateAccountFields(account, updatedAccount);

        Set<String> newCurrencies = updatedAccount.getCurrencies();
        validateCurrencies(newCurrencies);

        Set<String> oldCurrencies = account.getCurrencies();
        createNewBalancesForAccount(id, newCurrencies, oldCurrencies);
        removeOldBalancesForAccount(id, oldCurrencies, newCurrencies);

        account.setCurrencies(newCurrencies);
        Account savedAccount = accountRepository.save(account);
        log.info("Account updated: id={}", savedAccount.getId());
        return savedAccount;
    }

    @Override
    @Transactional
    public void deleteAccount(@NonNull Long id) {
        log.info("Deleting account: id={}", id);
        Account account = getAccountById(id);
        validateAccountCanBeDeleted(id);
        
        accountRepository.delete(account);
        log.info("Account deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountBalance> getAccountBalances(@NonNull Long accountId) {
        return accountBalanceRepository.findByAccountId(accountId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<AccountBalance>> getAccountBalancesBatch(@NonNull List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        List<AccountBalance> allBalances = accountBalanceRepository.findByAccountIdIn(accountIds);
        return allBalances.stream()
                .collect(Collectors.groupingBy(AccountBalance::getAccountId));
    }

    private void validateCurrencies(Set<String> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            throw new AccountException(ERROR_CODE_CURRENCIES_REQUIRED, "Account must have at least one currency");
        }
    }

    private void updateAccountFields(@NonNull Account account, @NonNull Account updatedAccount) {
        account.setName(updatedAccount.getName());
        account.setDescription(updatedAccount.getDescription());
        account.setUserId(updatedAccount.getUserId());
        account.setBranchId(updatedAccount.getBranchId());
    }

    private void createBalancesForAccount(@NonNull Long accountId, @NonNull Set<String> currencies) {
        List<AccountBalance> balances = currencies.stream()
                .map(currency -> createAccountBalance(accountId, currency))
                .toList();
        accountBalanceRepository.saveAll(balances);
    }

    private void createNewBalancesForAccount(@NonNull Long accountId, @NonNull Set<String> newCurrencies, @NonNull Set<String> oldCurrencies) {
        List<AccountBalance> balancesToCreate = newCurrencies.stream()
                .filter(currency -> !oldCurrencies.contains(currency))
                .map(currency -> createAccountBalance(accountId, currency))
                .toList();
        
        if (!balancesToCreate.isEmpty()) {
            accountBalanceRepository.saveAll(balancesToCreate);
        }
    }

    private Map<String, AccountBalance> buildBalanceMap(@NonNull Long accountId) {
        List<AccountBalance> allBalances = accountBalanceRepository.findByAccountId(accountId);
        return allBalances.stream()
                .collect(Collectors.toMap(
                        AccountBalance::getCurrency,
                        balance -> balance,
                        (existing, _) -> existing
                ));
    }

    private void removeOldBalancesForAccount(@NonNull Long accountId, @NonNull Set<String> oldCurrencies, @NonNull Set<String> newCurrencies) {
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

    private void validateAccountCanBeDeleted(@NonNull Long accountId) {
        long nonZeroCount = accountBalanceRepository.countNonZeroBalances(accountId);
        if (nonZeroCount > 0) {
            throw new AccountException(ERROR_CODE_BALANCE_NOT_ZERO, 
                    String.format("Cannot delete account: has %d non-zero balance(s)", nonZeroCount));
        }
    }

    private AccountBalance createAccountBalance(@NonNull Long accountId, @NonNull String currency) {
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
