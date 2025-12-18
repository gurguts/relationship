package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.exceptions.transaction.TransactionNotFoundException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCrudService implements ITransactionCrudService {
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        String.format("Transaction not found with id: %d", id)));
    }

    @Override
    @Transactional
    public void updateTransactionAmount(Long transactionId, BigDecimal amount) {
        if (transactionId == null || amount == null) {
            throw new TransactionException("Transaction ID and amount cannot be null");
        }

        Transaction transaction = getTransaction(transactionId);
        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        // Note: Balance updates are now handled by AccountTransactionService
        // This method only updates the transaction amount in the database
    }

    private final AccountTransactionService accountTransactionService;

    @Override
    public void delete(Long transactionId) {
        accountTransactionService.deleteTransaction(transactionId);
    }

}
