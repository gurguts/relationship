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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCrudService implements ITransactionCrudService {
    private final TransactionRepository transactionRepository;

    @Override
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

    @Override
    public void delete(Long transactionId) {
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if (transaction.isEmpty()) {
            throw new TransactionNotFoundException(String.format("Transaction not found with id: %d", transactionId));
        }

        Transaction t = transaction.get();
        
        // Note: Balance rollback is now handled by AccountTransactionService
        // This method only deletes the transaction from the database
        transactionRepository.delete(t);
    }

}
