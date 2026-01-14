package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.account.AccountBalance;

import java.util.List;
import java.util.Map;

public interface IAccountService {
    List<Account> getAccountsAccessibleToUser(@NonNull Long userId);
    
    List<Account> getAccountsByUserId(@NonNull Long userId);
    
    List<Account> getAccountsByBranchId(@NonNull Long branchId);
    
    Account getAccountById(@NonNull Long id);
    
    Account createAccount(@NonNull Account account);
    
    Account updateAccount(@NonNull Long id, @NonNull Account updatedAccount);
    
    void deleteAccount(@NonNull Long id);
    
    List<AccountBalance> getAccountBalances(@NonNull Long accountId);
    
    Map<Long, List<AccountBalance>> getAccountBalancesBatch(@NonNull List<Long> accountIds);
}
