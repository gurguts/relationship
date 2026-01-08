package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.exceptions.transaction.TransactionNotFoundException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.example.userservice.services.account.AccountBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCrudService implements ITransactionCrudService {
    private static final String ERROR_CODE_INVALID_AMOUNT = "INVALID_AMOUNT";
    private static final String DEFAULT_CURRENCY = "UAH";
    private static final int CONVERTED_AMOUNT_SCALE = 2;
    private static final RoundingMode CONVERTED_AMOUNT_ROUNDING_MODE = RoundingMode.HALF_UP;

    private final TransactionRepository transactionRepository;
    private final AccountTransactionService accountTransactionService;
    private final AccountBalanceService accountBalanceService;

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(@NonNull Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        String.format("Transaction not found with id: %d", id)));
    }

    @Override
    @Transactional
    public void updateTransactionAmount(@NonNull Long transactionId, @NonNull BigDecimal amount) {
        validateAmount(amount);
        
        Transaction transaction = getTransaction(transactionId);
        BigDecimal oldAmount = transaction.getAmount();
        BigDecimal difference = amount.subtract(oldAmount);

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Amount unchanged for transaction: id={}, amount={}", transactionId, amount);
            return;
        }

        transaction.setAmount(amount);
        transactionRepository.save(transaction);

        updateAccountBalanceForTransaction(transaction, oldAmount, amount, difference);
        
        log.info("Updated transaction amount: id={}, oldAmount={}, newAmount={}, difference={}", 
                transactionId, oldAmount, amount, difference);
    }
    
    private void updateAccountBalanceForTransaction(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount, 
                                                   @NonNull BigDecimal newAmount, @NonNull BigDecimal difference) {
        TransactionType type = transaction.getType();
        if (type == null) {
            log.warn("Transaction type is null for transaction: id={}", transaction.getId());
            return;
        }

        String currency = transaction.getCurrency() != null ? transaction.getCurrency() : DEFAULT_CURRENCY;
        
        switch (type) {
            case CLIENT_PAYMENT:
            case EXTERNAL_EXPENSE:
            case VEHICLE_EXPENSE:
                updateBalanceForClientPaymentOrExpense(transaction, currency, difference);
                break;
            case EXTERNAL_INCOME:
                updateBalanceForExternalIncome(transaction, currency, difference);
                break;
            case INTERNAL_TRANSFER:
                updateBalanceForInternalTransfer(transaction, currency, difference);
                break;
            case CURRENCY_CONVERSION:
                updateBalanceForCurrencyConversion(transaction, oldAmount, newAmount, difference);
                break;
            default:
                log.debug("No balance update needed for transaction type: {}", type);
                break;
        }
    }

    private void updateBalanceForClientPaymentOrExpense(@NonNull Transaction transaction, @NonNull String currency, 
                                                        @NonNull BigDecimal difference) {
        if (transaction.getFromAccountId() == null) {
            log.warn("FromAccountId is null for transaction: id={}, type={}", transaction.getId(), transaction.getType());
            return;
        }
        
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            accountBalanceService.subtractAmount(transaction.getFromAccountId(), currency, difference);
        } else {
            accountBalanceService.addAmount(transaction.getFromAccountId(), currency, difference.negate());
        }
    }

    private void updateBalanceForExternalIncome(@NonNull Transaction transaction, @NonNull String currency, 
                                                @NonNull BigDecimal difference) {
        if (transaction.getToAccountId() == null) {
            log.warn("ToAccountId is null for transaction: id={}, type={}", transaction.getId(), transaction.getType());
            return;
        }
        
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            accountBalanceService.addAmount(transaction.getToAccountId(), currency, difference);
        } else {
            accountBalanceService.subtractAmount(transaction.getToAccountId(), currency, difference.negate());
        }
    }

    private void updateBalanceForInternalTransfer(@NonNull Transaction transaction, @NonNull String currency, 
                                                  @NonNull BigDecimal difference) {
        if (transaction.getFromAccountId() == null || transaction.getToAccountId() == null) {
            log.warn("FromAccountId or ToAccountId is null for transaction: id={}, type={}", 
                    transaction.getId(), transaction.getType());
            return;
        }
        
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            accountBalanceService.subtractAmount(transaction.getFromAccountId(), currency, difference);
            accountBalanceService.addAmount(transaction.getToAccountId(), currency, difference);
        } else {
            accountBalanceService.addAmount(transaction.getFromAccountId(), currency, difference.negate());
            accountBalanceService.subtractAmount(transaction.getToAccountId(), currency, difference.negate());
        }
    }

    private void updateBalanceForCurrencyConversion(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount, 
                                                    @NonNull BigDecimal newAmount, @NonNull BigDecimal difference) {
        if (transaction.getFromAccountId() == null) {
            log.warn("FromAccountId is null for currency conversion transaction: id={}", transaction.getId());
            return;
        }
        
        String fromCurrency = transaction.getCurrency();
        String toCurrency = transaction.getConvertedCurrency();
        
        if (fromCurrency == null || toCurrency == null) {
            log.warn("Currency or convertedCurrency is null for transaction: id={}", transaction.getId());
            return;
        }
        
        BigDecimal exchangeRate = transaction.getExchangeRate();
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid exchange rate for transaction: id={}, exchangeRate={}", transaction.getId(), exchangeRate);
            return;
        }
        
        BigDecimal newConvertedAmount = newAmount.divide(exchangeRate, CONVERTED_AMOUNT_SCALE, CONVERTED_AMOUNT_ROUNDING_MODE);
        BigDecimal oldConvertedAmount = oldAmount.divide(exchangeRate, CONVERTED_AMOUNT_SCALE, CONVERTED_AMOUNT_ROUNDING_MODE);
        BigDecimal convertedDifference = newConvertedAmount.subtract(oldConvertedAmount);
        
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            accountBalanceService.subtractAmount(transaction.getFromAccountId(), fromCurrency, difference);
            accountBalanceService.addAmount(transaction.getFromAccountId(), toCurrency, convertedDifference);
        } else {
            accountBalanceService.addAmount(transaction.getFromAccountId(), fromCurrency, difference.negate());
            accountBalanceService.subtractAmount(transaction.getFromAccountId(), toCurrency, convertedDifference.negate());
        }
    }

    private void validateAmount(@NonNull BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException(ERROR_CODE_INVALID_AMOUNT, "Transaction amount must be greater than zero");
        }
    }

    @Override
    public void delete(@NonNull Long transactionId) {
        accountTransactionService.deleteTransaction(transactionId);
    }
}
