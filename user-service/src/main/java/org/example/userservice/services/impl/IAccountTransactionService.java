package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IAccountTransactionService {
    Transaction getTransactionById(@NonNull Long transactionId);
    
    List<Transaction> getTransactionsByVehicleId(@NonNull Long vehicleId);
    
    Map<Long, List<Transaction>> getTransactionsByVehicleIds(@NonNull List<Long> vehicleIds);
    
    void deleteTransactionsByVehicleId(@NonNull Long vehicleId);
    
    Transaction createTransaction(@NonNull Transaction transaction);
    
    Transaction updateTransaction(@NonNull Long transactionId, Long categoryId, String description, 
                                   BigDecimal newAmount, BigDecimal newExchangeRate, BigDecimal newCommission, 
                                   BigDecimal newConvertedAmount, Long counterpartyId);
    
    void deleteTransaction(@NonNull Long transactionId);
}
