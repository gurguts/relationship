package org.example.transactionservice.services.impl;

import org.example.transactionservice.models.Transaction;

import java.math.BigDecimal;

public interface ITransactionCrudService {
    Transaction getTransaction(Long id);

    void updateTransactionAmount(Long transactionId, BigDecimal amount);

    Transaction createSaleTransaction(Transaction transaction, Long productId);

    Transaction createPurchaseTransaction(Transaction transaction, Long productId);

    Transaction createDepositTransaction(Transaction transaction);

    Transaction createWithdrawTransaction(Transaction transaction);
}
