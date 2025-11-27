package org.example.userservice.services.impl;

import org.example.userservice.models.transaction.Transaction;

import java.math.BigDecimal;

public interface ITransactionCrudService {
    Transaction getTransaction(Long id);

    void updateTransactionAmount(Long transactionId, BigDecimal amount);

    void delete(Long transactionId);
}
