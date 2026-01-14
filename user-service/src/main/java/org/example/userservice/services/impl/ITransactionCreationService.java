package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;

public interface ITransactionCreationService {
    Transaction createInternalTransfer(@NonNull Transaction transaction);
    
    Transaction createExternalIncome(@NonNull Transaction transaction);
    
    Transaction createExternalExpense(@NonNull Transaction transaction);
    
    Transaction createClientPayment(@NonNull Transaction transaction);
    
    Transaction createCurrencyConversion(@NonNull Transaction transaction);
}
