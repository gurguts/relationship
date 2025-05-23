package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.clients.ProductApiClient;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.exceptions.transaction.TransactionNotFoundException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.IBalanceService;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCrudService implements ITransactionCrudService {
    private final TransactionRepository transactionRepository;
    private final IBalanceService balanceService;
    private final ProductApiClient productApiClient;

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
        BigDecimal currentAmount = transaction.getAmount();

        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        BigDecimal difference = currentAmount.subtract(amount);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            Long userId = transaction.getTargetUserId();
            if (userId == null) {
                throw new TransactionException(
                        String.format("User ID is missing in transaction with ID: %d", transactionId));
            }
            try {
                balanceService.updateUserBalance(userId, difference, transaction.getCurrency());
            } catch (Exception e) {
                throw new TransactionException("USER_BALANCE",
                        String.format("Failed to update user balance for userId: %d", userId));
            }
        }
    }

    @Override
    @Transactional
    public Transaction createSaleTransaction(Transaction transaction, Long productId) {
        balanceService.updateUserBalance(transaction.getTargetUserId(), transaction.getAmount(),
                transaction.getCurrency());
        String productName = productApiClient.getProduct(productId);
        transaction.setDescription(String.format("Продаж товара: %s", productName));
        transaction.setType(TransactionType.DEPOSIT);
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction createPurchaseTransaction(Transaction transaction, Long productId) {
        balanceService.updateUserBalance(transaction.getTargetUserId(), transaction.getAmount().negate(),
                transaction.getCurrency());
        String productName = productApiClient.getProduct(productId);
        transaction.setDescription(String.format("Закупка товара: %s", productName));
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

        balanceService.updateUserBalance(transaction.getTargetUserId(), transaction.getAmount(),
                transaction.getCurrency());
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction createWithdrawTransaction(Transaction transaction) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        transaction.setExecutorUserId(userId);

        transaction.setType(TransactionType.WITHDRAWAL);

        balanceService.updateUserBalance(transaction.getTargetUserId(), transaction.getAmount().negate(),
                transaction.getCurrency());
        return transactionRepository.save(transaction);
    }

    @Override
    public void delete(Long transactionId) {
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if (transaction.isEmpty()) {
            throw new TransactionNotFoundException(String.format("Transaction not found with id: %d", transactionId));
        }

        if (transaction.get().getType().equals(TransactionType.WITHDRAWAL)) {
            balanceService.updateUserBalance(transaction.get().getTargetUserId(), transaction.get().getAmount(),
                    transaction.get().getCurrency());

        } else {
            balanceService.updateUserBalance(transaction.get().getTargetUserId(),
                    transaction.get().getAmount().negate(), transaction.get().getCurrency());
        }

        transactionRepository.delete(transaction.get());
    }
}
