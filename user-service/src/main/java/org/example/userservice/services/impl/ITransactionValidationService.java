package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;

import java.math.BigDecimal;

public interface ITransactionValidationService {
    void validateAccount(@NonNull Long accountId);
    
    void validateAccounts(@NonNull Long fromAccountId, @NonNull Long toAccountId);
    
    void validateCurrency(@NonNull Long accountId, @NonNull String currency);
    
    void validateCommission(BigDecimal commission, BigDecimal amount);
    
    void validateSameAccounts(@NonNull Long fromAccountId, @NonNull Long toAccountId);
    
    void checkAccountPermissions(@NonNull Long userId, @NonNull Transaction transaction);
}
