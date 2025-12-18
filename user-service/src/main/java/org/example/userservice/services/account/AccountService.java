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

        // Create AccountBalance entries for each currency
        for (String currency : savedAccount.getCurrencies()) {
            AccountBalance balance = new AccountBalance();
            balance.setAccountId(savedAccount.getId());
            balance.setCurrency(currency.toUpperCase());
            balance.setAmount(BigDecimal.ZERO);
            accountBalanceRepository.save(balance);
        }

        return savedAccount;
    }

    @Transactional
    public Account updateAccount(Long id, Account updatedAccount) {
        Account account = getAccountById(id);
        account.setName(updatedAccount.getName());
        account.setDescription(updatedAccount.getDescription());
        account.setUserId(updatedAccount.getUserId());
        account.setBranchId(updatedAccount.getBranchId());

        // Handle currency changes
        Set<String> newCurrencies = updatedAccount.getCurrencies();
        Set<String> oldCurrencies = account.getCurrencies();

        // Add new currencies
        for (String currency : newCurrencies) {
            if (!oldCurrencies.contains(currency)) {
                AccountBalance balance = new AccountBalance();
                balance.setAccountId(account.getId());
                balance.setCurrency(currency.toUpperCase());
                balance.setAmount(BigDecimal.ZERO);
                accountBalanceRepository.save(balance);
            }
        }

        // Remove old currencies (only if balance is zero)
        for (String currency : oldCurrencies) {
            if (!newCurrencies.contains(currency)) {
                AccountBalance balance = accountBalanceRepository
                        .findByAccountIdAndCurrency(account.getId(), currency)
                        .orElse(null);
                if (balance != null && balance.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    throw new AccountException("CURRENCY", 
                            String.format("Cannot remove currency %s: account has non-zero balance", currency));
                }
                if (balance != null) {
                    accountBalanceRepository.delete(balance);
                }
            }
        }

        account.setCurrencies(newCurrencies);
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = getAccountById(id);
        // Check if all balances are zero
        List<AccountBalance> balances = accountBalanceRepository.findByAccountId(id);
        for (AccountBalance balance : balances) {
            if (balance.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                throw new AccountException("BALANCE", 
                        String.format("Cannot delete account: has non-zero balance in %s", balance.getCurrency()));
            }
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

