package org.example.transactionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.clients.BalanceApiClient;
import org.example.transactionservice.clients.ProductApiClient;
import org.example.transactionservice.exceptions.TransactionException;
import org.example.transactionservice.exceptions.TransactionNotFoundException;
import org.example.transactionservice.models.Transaction;
import org.example.transactionservice.models.TransactionType;
import org.example.transactionservice.models.dto.BalanceUpdateDTO;
import org.example.transactionservice.repositories.TransactionRepository;
import org.example.transactionservice.services.impl.ITransactionCrudService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCrudService implements ITransactionCrudService {
    private final TransactionRepository transactionRepository;
    private final BalanceApiClient balanceApiClient;
    private final ProductApiClient productApiClient;

    @Override
    public Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));
    }

    @Override
    @Transactional
    public void updateTransactionAmount(Long transactionId, BigDecimal amount) {
        if (transactionId == null || amount == null) {
            throw new TransactionException("Transaction ID and amount cannot be null");
        }

        Transaction transaction = getTransaction(transactionId);
        BigDecimal currentAmount = transaction.getAmount();

        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        BigDecimal difference = currentAmount.subtract(amount);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            Long userId = transaction.getTargetUserId();
            if (userId == null) {
                throw new TransactionException("User ID is missing in transaction with ID: " + transactionId);
            }
            try {
                balanceApiClient.updateUserBalance(userId, new BalanceUpdateDTO(difference, transaction.getCurrency()));
            } catch (Exception e) {
                throw new TransactionException("USER_BALANCE", "Failed to update user balance for userId: " + userId);
            }
        }
    }

    @Override
    @Transactional
    public Transaction createSaleTransaction(Transaction transaction, Long productId) {
        balanceApiClient.updateUserBalance(transaction.getTargetUserId(), new BalanceUpdateDTO(transaction.getAmount(), transaction.getCurrency()));
        String productName = productApiClient.getProduct(productId);
        transaction.setDescription("Продаж товара: " + productName);
        transaction.setType(TransactionType.DEPOSIT);
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction createPurchaseTransaction(Transaction transaction, Long productId) {
        balanceApiClient.updateUserBalance(transaction.getTargetUserId(), new BalanceUpdateDTO(transaction.getAmount().negate(), transaction.getCurrency()));
        String productName = productApiClient.getProduct(productId);
        transaction.setDescription("Закупка товара: " + productName);
        transaction.setType(TransactionType.WITHDRAWAL);
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction createDepositTransaction(Transaction transaction) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        transaction.setExecutorUserId(userId);

        transaction.setType(TransactionType.DEPOSIT);

        balanceApiClient.updateUserBalance(transaction.getTargetUserId(), new BalanceUpdateDTO(transaction.getAmount(), transaction.getCurrency()));
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction createWithdrawTransaction(Transaction transaction) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        transaction.setExecutorUserId(userId);

        transaction.setType(TransactionType.WITHDRAWAL);

        balanceApiClient.updateUserBalance(transaction.getTargetUserId(), new BalanceUpdateDTO(transaction.getAmount().negate(), transaction.getCurrency()));
        return transactionRepository.save(transaction);
    }
}
