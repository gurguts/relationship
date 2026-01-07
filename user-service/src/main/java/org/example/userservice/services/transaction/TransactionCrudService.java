package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.exceptions.transaction.TransactionNotFoundException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.example.userservice.services.account.AccountBalanceService;
import org.example.userservice.models.transaction.TransactionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCrudService implements ITransactionCrudService {
    private final TransactionRepository transactionRepository;
    private final AccountTransactionService accountTransactionService;
    private final AccountBalanceService accountBalanceService;

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(@NotNull Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        String.format("Transaction not found with id: %d", id)));
    }

    @Override
    @Transactional
    public void updateTransactionAmount(@NotNull Long transactionId, @NotNull BigDecimal amount) {
        if (transactionId == null || amount == null) {
            throw new TransactionException("INVALID_INPUT", "Transaction ID and amount cannot be null");
        }

        Transaction transaction = getTransaction(transactionId);
        BigDecimal oldAmount = transaction.getAmount();
        BigDecimal difference = amount.subtract(oldAmount);
        
        // Update transaction amount
        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        // Update account balance based on transaction type
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            updateAccountBalanceForTransaction(transaction, oldAmount, amount, difference);
        }
    }
    
    private void updateAccountBalanceForTransaction(Transaction transaction, BigDecimal oldAmount, BigDecimal newAmount, BigDecimal difference) {
        TransactionType type = transaction.getType();
        String currency = transaction.getCurrency() != null ? transaction.getCurrency() : "UAH";
        
        switch (type) {
            case CLIENT_PAYMENT:
            case EXTERNAL_EXPENSE:
            case VEHICLE_EXPENSE:
                // These transaction types subtract from fromAccountId
                if (transaction.getFromAccountId() != null) {
                    if (difference.compareTo(BigDecimal.ZERO) > 0) {
                        // New amount is greater - subtract additional amount
                        accountBalanceService.subtractAmount(transaction.getFromAccountId(), currency, difference);
                    } else {
                        // New amount is smaller - add back the difference (difference is negative, so we negate it)
                        accountBalanceService.addAmount(transaction.getFromAccountId(), currency, difference.negate());
                    }
                }
                break;
                
            case EXTERNAL_INCOME:
                // This transaction type adds to toAccountId
                if (transaction.getToAccountId() != null) {
                    if (difference.compareTo(BigDecimal.ZERO) > 0) {
                        // New amount is greater - add additional amount
                        accountBalanceService.addAmount(transaction.getToAccountId(), currency, difference);
                    } else {
                        // New amount is smaller - subtract the difference (difference is negative, so we negate it)
                        accountBalanceService.subtractAmount(transaction.getToAccountId(), currency, difference.negate());
                    }
                }
                break;
                
            case INTERNAL_TRANSFER:
                // For internal transfers, we need to update both accounts
                if (transaction.getFromAccountId() != null && transaction.getToAccountId() != null) {
                    if (difference.compareTo(BigDecimal.ZERO) > 0) {
                        // New amount is greater - subtract additional from source, add to destination
                        accountBalanceService.subtractAmount(transaction.getFromAccountId(), currency, difference);
                        accountBalanceService.addAmount(transaction.getToAccountId(), currency, difference);
                    } else {
                        // New amount is smaller - add back to source, subtract from destination
                        accountBalanceService.addAmount(transaction.getFromAccountId(), currency, difference.negate());
                        accountBalanceService.subtractAmount(transaction.getToAccountId(), currency, difference.negate());
                    }
                }
                break;
                
            case CURRENCY_CONVERSION:
                // For currency conversion, we need to handle both currencies
                if (transaction.getFromAccountId() != null && transaction.getToAccountId() != null) {
                    String fromCurrency = transaction.getCurrency();
                    String toCurrency = transaction.getConvertedCurrency();
                    
                    if (fromCurrency != null && toCurrency != null) {
                        // Recalculate converted amount based on new amount
                        BigDecimal exchangeRate = transaction.getExchangeRate();
                        if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal newConvertedAmount = newAmount.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP);
                            BigDecimal oldConvertedAmount = oldAmount.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP);
                            BigDecimal convertedDifference = newConvertedAmount.subtract(oldConvertedAmount);
                            
                            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                                accountBalanceService.subtractAmount(transaction.getFromAccountId(), fromCurrency, difference);
                                accountBalanceService.addAmount(transaction.getToAccountId(), toCurrency, convertedDifference);
                            } else {
                                accountBalanceService.addAmount(transaction.getFromAccountId(), fromCurrency, difference.negate());
                                accountBalanceService.subtractAmount(transaction.getToAccountId(), toCurrency, convertedDifference.negate());
                            }
                        }
                    }
                }
                break;
                
            default:
                // For other transaction types, do nothing
                break;
        }
    }

    @Override
    public void delete(@NotNull Long transactionId) {
        accountTransactionService.deleteTransaction(transactionId);
    }

}
