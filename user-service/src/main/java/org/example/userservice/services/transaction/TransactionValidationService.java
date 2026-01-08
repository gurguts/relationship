package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.services.account.AccountBalanceService;
import org.example.userservice.services.branch.BranchPermissionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransactionValidationService {
    private static final String ERROR_CODE_CURRENCY_REQUIRED = "CURRENCY_REQUIRED";
    private static final String ERROR_CODE_INVALID_COMMISSION = "INVALID_COMMISSION";
    private static final String ERROR_CODE_SAME_ACCOUNTS = "SAME_ACCOUNTS";
    private static final String ERROR_CODE_ACCESS_DENIED = "ACCESS_DENIED";

    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final BranchPermissionService branchPermissionService;

    public void validateAccount(@NonNull Long accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account with ID %d not found", accountId)));
    }

    public void validateAccounts(@NonNull Long fromAccountId, @NonNull Long toAccountId) {
        validateAccount(fromAccountId);
        validateAccount(toAccountId);
    }

    public void validateCurrency(@NonNull Long accountId, @NonNull String currency) {
        if (currency.trim().isEmpty()) {
            throw new TransactionException(ERROR_CODE_CURRENCY_REQUIRED, "Currency is required");
        }
        try {
            accountBalanceService.getBalance(accountId, currency);
        } catch (AccountNotFoundException e) {
            throw new AccountException("CURRENCY",
                    String.format("Currency %s is not supported for account %d", currency, accountId));
        }
    }

    public void validateCommission(BigDecimal commission, BigDecimal amount) {
        if (commission != null) {
            if (commission.compareTo(BigDecimal.ZERO) < 0) {
                throw new TransactionException(ERROR_CODE_INVALID_COMMISSION, "Commission cannot be negative");
            }
            if (commission.compareTo(amount) >= 0) {
                throw new TransactionException(ERROR_CODE_INVALID_COMMISSION, "Commission cannot be greater than or equal to the transfer amount");
            }
        }
    }

    public void validateSameAccounts(@NonNull Long fromAccountId, @NonNull Long toAccountId) {
        if (fromAccountId.equals(toAccountId)) {
            throw new TransactionException(ERROR_CODE_SAME_ACCOUNTS, "Source and destination accounts cannot be the same");
        }
    }

    public void checkAccountPermissions(@NonNull Long userId, @NonNull Transaction transaction) {
        TransactionType type = transaction.getType();
        
        if (type == TransactionType.INTERNAL_TRANSFER || type == TransactionType.CURRENCY_CONVERSION) {
            if (transaction.getFromAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getFromAccountId());
            }
            if (transaction.getToAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getToAccountId());
            }
        } else if (type == TransactionType.EXTERNAL_INCOME) {
            if (transaction.getToAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getToAccountId());
            }
        } else if (type == TransactionType.EXTERNAL_EXPENSE || type == TransactionType.CLIENT_PAYMENT || type == TransactionType.VEHICLE_EXPENSE) {
            if (transaction.getFromAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getFromAccountId());
            }
        }
    }

    private void checkAccountOperatePermission(@NonNull Long userId, @NonNull Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account with ID %d not found", accountId)));

        if (account.getBranchId() == null) {
            return;
        }

        if (!branchPermissionService.canOperate(userId, account.getBranchId())) {
            throw new TransactionException(ERROR_CODE_ACCESS_DENIED,
                    String.format("User does not have permission to operate on account %d (branch %d)", 
                            accountId, account.getBranchId()));
        }
    }
}

