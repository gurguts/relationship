package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.IAccountBalanceService;
import org.example.userservice.services.impl.ITransactionCreationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCreationService implements ITransactionCreationService {
    private static final String ERROR_CODE_CLIENT_ID_REQUIRED = "CLIENT_ID_REQUIRED";
    private static final String ERROR_CODE_INVALID_CURRENCY_CONVERSION = "INVALID_CURRENCY_CONVERSION";
    private static final String ERROR_CODE_SAME_CURRENCIES = "SAME_CURRENCIES";
    private static final String ERROR_CODE_INVALID_AMOUNT = "INVALID_AMOUNT";
    private static final String ERROR_CODE_EXCHANGE_RATE_OR_AMOUNT_REQUIRED = "EXCHANGE_RATE_OR_AMOUNT_REQUIRED";
    private static final int EXCHANGE_RATE_SCALE = 6;
    private static final int CONVERTED_AMOUNT_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final IAccountBalanceService accountBalanceService;
    private final TransactionValidationService validationService;

    @Override
    @Transactional
    public Transaction createInternalTransfer(@NonNull Transaction transaction) {
        log.info("Creating internal transfer transaction: fromAccountId={}, toAccountId={}, amount={}", 
                transaction.getFromAccountId(), transaction.getToAccountId(), transaction.getAmount());
        validationService.validateAccounts(transaction.getFromAccountId(), transaction.getToAccountId());
        validationService.validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());
        validationService.validateCurrency(transaction.getToAccountId(), transaction.getCurrency());
        validationService.validateSameAccounts(transaction.getFromAccountId(), transaction.getToAccountId());

        BigDecimal amount = transaction.getAmount();
        BigDecimal commission = transaction.getCommission();
        
        validationService.validateCommission(commission, amount);

        BigDecimal transferAmount = commission != null ? amount.subtract(commission) : amount;

        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                transaction.getCurrency(),
                amount
        );

        accountBalanceService.addAmount(
                transaction.getToAccountId(),
                transaction.getCurrency(),
                transferAmount
        );

        Transaction saved = transactionRepository.save(transaction);
        log.info("Internal transfer transaction created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Transaction createExternalIncome(@NonNull Transaction transaction) {
        log.info("Creating external income transaction: toAccountId={}, amount={}", 
                transaction.getToAccountId(), transaction.getAmount());
        validationService.validateAccount(transaction.getToAccountId());
        validationService.validateCurrency(transaction.getToAccountId(), transaction.getCurrency());

        accountBalanceService.addAmount(
                transaction.getToAccountId(),
                transaction.getCurrency(),
                transaction.getAmount()
        );

        Transaction saved = transactionRepository.save(transaction);
        log.info("External income transaction created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Transaction createExternalExpense(@NonNull Transaction transaction) {
        log.info("Creating external expense transaction: fromAccountId={}, amount={}", 
                transaction.getFromAccountId(), transaction.getAmount());
        validationService.validateAccount(transaction.getFromAccountId());
        validationService.validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());

        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                transaction.getCurrency(),
                transaction.getAmount()
        );

        Transaction saved = transactionRepository.save(transaction);
        log.info("External expense transaction created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Transaction createClientPayment(@NonNull Transaction transaction) {
        log.info("Creating client payment transaction: fromAccountId={}, clientId={}, amount={}", 
                transaction.getFromAccountId(), transaction.getClientId(), transaction.getAmount());
        validationService.validateAccount(transaction.getFromAccountId());
        validationService.validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());

        if (transaction.getClientId() == null) {
            throw new TransactionException(ERROR_CODE_CLIENT_ID_REQUIRED, "Client ID is required for client payment");
        }

        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                transaction.getCurrency(),
                transaction.getAmount()
        );

        transaction.setToAccountId(null);

        Transaction saved = transactionRepository.save(transaction);
        log.info("Client payment transaction created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Transaction createCurrencyConversion(@NonNull Transaction transaction) {
        log.info("Creating currency conversion transaction: fromAccountId={}, amount={}", 
                transaction.getFromAccountId(), transaction.getAmount());
        validationService.validateAccount(transaction.getFromAccountId());
        
        if (!transaction.getFromAccountId().equals(transaction.getToAccountId())) {
            throw new TransactionException(ERROR_CODE_INVALID_CURRENCY_CONVERSION, "Currency conversion must be within the same account");
        }

        String fromCurrency = transaction.getCurrency();
        String toCurrency = transaction.getConvertedCurrency();
        
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            throw new TransactionException(ERROR_CODE_SAME_CURRENCIES, "Source and destination currencies must be different");
        }

        validationService.validateCurrency(transaction.getFromAccountId(), fromCurrency);
        validationService.validateCurrency(transaction.getFromAccountId(), toCurrency);

        BigDecimal convertedAmount = transaction.getConvertedAmount();
        BigDecimal exchangeRate = transaction.getExchangeRate();
        
        if (convertedAmount != null && convertedAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionException(ERROR_CODE_INVALID_AMOUNT, "Amount must be greater than zero");
            }
            exchangeRate = transaction.getAmount().divide(convertedAmount, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            transaction.setExchangeRate(exchangeRate);
        } else if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            convertedAmount = transaction.getAmount()
                    .divide(exchangeRate, CONVERTED_AMOUNT_SCALE, RoundingMode.HALF_UP);
            transaction.setConvertedAmount(convertedAmount);
        } else {
            throw new TransactionException(ERROR_CODE_EXCHANGE_RATE_OR_AMOUNT_REQUIRED, "Either exchange rate or converted amount must be provided");
        }

        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                fromCurrency,
                transaction.getAmount()
        );

        accountBalanceService.addAmount(
                transaction.getFromAccountId(),
                toCurrency,
                convertedAmount
        );

        Transaction saved = transactionRepository.save(transaction);
        log.info("Currency conversion transaction created: id={}", saved.getId());
        return saved;
    }
}

