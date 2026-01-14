package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;

import java.math.BigDecimal;

public interface ITransactionUpdateService {
    Transaction updateTransaction(@NonNull Long transactionId, Long categoryId, String description, 
                                   BigDecimal newAmount, BigDecimal newExchangeRate, BigDecimal newCommission, 
                                   BigDecimal newConvertedAmount, Long counterpartyId);
}
