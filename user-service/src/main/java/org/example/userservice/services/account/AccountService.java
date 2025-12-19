package org.example.userservice.services.account;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.repositories.AccountBalanceRepository;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.services.branch.BranchPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final BranchPermissionService branchPermissionService;

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsAccessibleToUser(Long userId) {
        List<Account> allAccounts = accountRepository.findAllByOrderByNameAsc();
        
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(userId);
        java.util.Set<Long> accessibleBranchIds = userPermissions.stream()
                .filter(org.example.userservice.models.branch.BranchPermission::getCanView)
                .map(org.example.userservice.models.branch.BranchPermission::getBranchId)
                .collect(Collectors.toSet());
        
        return allAccounts.stream()
                .filter(account -> {
                    if (account.getBranchId() == null) {
                        return true;
                    }
                    return accessibleBranchIds.contains(account.getBranchId());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByBranchId(Long branchId) {
        return accountRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public List<Account> getStandaloneAccounts() {
        return accountRepository.findByUserIdIsNull();
    }

    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account with ID %d not found", id)));
    }

    @Transactional
    public Account createAccount(Account account) {
        if (account.getCurrencies() == null || account.getCurrencies().isEmpty()) {
            throw new AccountException("CURRENCIES", "Account must have at least one currency");
        }

        Account savedAccount = accountRepository.save(account);

        List<AccountBalance> balances = savedAccount.getCurrencies().stream()
                .map(currency -> {
                    AccountBalance balance = new AccountBalance();
                    balance.setAccountId(savedAccount.getId());
                    balance.setCurrency(currency.toUpperCase());
                    balance.setAmount(BigDecimal.ZERO);
                    return balance;
                })
                .collect(Collectors.toList());
        accountBalanceRepository.saveAll(balances);

        return savedAccount;
    }

    @Transactional
    public Account updateAccount(Long id, Account updatedAccount) {
        Account account = getAccountById(id);
        account.setName(updatedAccount.getName());
        account.setDescription(updatedAccount.getDescription());
        account.setUserId(updatedAccount.getUserId());
        account.setBranchId(updatedAccount.getBranchId());

        Set<String> newCurrencies = updatedAccount.getCurrencies();
        Set<String> oldCurrencies = account.getCurrencies();

        List<AccountBalance> balancesToCreate = newCurrencies.stream()
                .filter(currency -> !oldCurrencies.contains(currency))
                .map(currency -> {
                    AccountBalance balance = new AccountBalance();
                    balance.setAccountId(account.getId());
                    balance.setCurrency(currency.toUpperCase());
                    balance.setAmount(BigDecimal.ZERO);
                    return balance;
                })
                .collect(Collectors.toList());
        
        if (!balancesToCreate.isEmpty()) {
            accountBalanceRepository.saveAll(balancesToCreate);
        }

        List<AccountBalance> allBalances = accountBalanceRepository.findByAccountId(account.getId());
        java.util.Map<String, AccountBalance> balanceMap = allBalances.stream()
                .collect(Collectors.toMap(
                        AccountBalance::getCurrency,
                        balance -> balance,
                        (existing, replacement) -> existing
                ));

        List<AccountBalance> balancesToDelete = oldCurrencies.stream()
                .filter(currency -> !newCurrencies.contains(currency))
                .map(balanceMap::get)
                .filter(balance -> {
                    if (balance != null && balance.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                        throw new AccountException("CURRENCY", 
                                String.format("Cannot remove currency %s: account has non-zero balance", balance.getCurrency()));
                    }
                    return balance != null;
                })
                .collect(Collectors.toList());
        
        if (!balancesToDelete.isEmpty()) {
            accountBalanceRepository.deleteAll(balancesToDelete);
        }

        account.setCurrencies(newCurrencies);
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = getAccountById(id);
        
        long nonZeroCount = accountBalanceRepository.countNonZeroBalances(id);
        if (nonZeroCount > 0) {
            throw new AccountException("BALANCE", 
                    String.format("Cannot delete account: has %d non-zero balance(s)", nonZeroCount));
        }
        
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public List<AccountBalance> getAccountBalances(Long accountId) {
        return accountBalanceRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public AccountBalance getAccountBalance(Long accountId, String currency) {
        return accountBalanceRepository.findByAccountIdAndCurrency(accountId, currency.toUpperCase())
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account balance for account %d and currency %s not found", accountId, currency)));
    }
}

