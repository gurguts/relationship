package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionUpdateService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionUpdateService implements ITransactionUpdateService {
    private static final String ERROR_CODE_INVALID_COMMISSION = "INVALID_COMMISSION";

    private final TransactionRepository transactionRepository;
    private final TransactionUpdateFieldsHandler fieldsHandler;
    private final TransactionUpdateBalanceEffectsHandler balanceEffectsHandler;

    @Override
    @Transactional
    public Transaction updateTransaction(@NonNull Long transactionId, Long categoryId, String description,
                                        BigDecimal newAmount, BigDecimal newExchangeRate, BigDecimal newCommission,
                                        BigDecimal newConvertedAmount, Long counterpartyId) {
        log.info("Updating transaction: id={}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));

        BigDecimal oldAmount = transaction.getAmount();
        BigDecimal oldCommission = transaction.getCommission();
        BigDecimal oldExchangeRate = transaction.getExchangeRate();
        BigDecimal oldConvertedAmount = transaction.getConvertedAmount();
        TransactionType type = transaction.getType();

        fieldsHandler.updateTransactionFields(transaction, categoryId, description, counterpartyId, type);

        boolean commissionChanged = updateCommissionIfNeeded(transaction, type, newCommission, newAmount, oldAmount, oldCommission);
        boolean exchangeRateChanged = updateExchangeRateIfNeeded(transaction, type, newExchangeRate, oldExchangeRate);
        boolean amountChanged = updateAmountIfNeeded(transaction, newAmount, oldAmount);
        boolean convertedAmountChanged = checkConvertedAmountChanged(type, newConvertedAmount, oldConvertedAmount);

        if (amountChanged || exchangeRateChanged || commissionChanged || convertedAmountChanged) {
            balanceEffectsHandler.applyBalanceEffects(transaction, type, oldAmount, oldCommission, newAmount, newCommission,
                    oldExchangeRate, newExchangeRate, oldConvertedAmount, newConvertedAmount,
                    amountChanged, commissionChanged, exchangeRateChanged, convertedAmountChanged);
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction updated: id={}", saved.getId());
        return saved;
    }

    private boolean updateCommissionIfNeeded(@NonNull Transaction transaction, @NonNull TransactionType type,
                                             BigDecimal newCommission, BigDecimal newAmount,
                                             @NonNull BigDecimal oldAmount, BigDecimal oldCommission) {
        if (type != TransactionType.INTERNAL_TRANSFER) {
            return false;
        }

        if (newCommission != null) {
            if (newCommission.compareTo(BigDecimal.ZERO) < 0) {
                throw new TransactionException(ERROR_CODE_INVALID_COMMISSION, "Commission cannot be negative");
            }
            BigDecimal currentAmount = newAmount != null ? newAmount : oldAmount;
            if (newCommission.compareTo(currentAmount) >= 0) {
                throw new TransactionException(ERROR_CODE_INVALID_COMMISSION, "Commission cannot be greater than or equal to the transfer amount");
            }
            if (oldCommission == null || newCommission.compareTo(oldCommission) != 0) {
                transaction.setCommission(newCommission);
                return true;
            }
        } else if (oldCommission != null) {
            transaction.setCommission(null);
            return true;
        }
        return false;
    }

    private boolean updateExchangeRateIfNeeded(@NonNull Transaction transaction, @NonNull TransactionType type,
                                               BigDecimal newExchangeRate, BigDecimal oldExchangeRate) {
        if ((type == TransactionType.CURRENCY_CONVERSION || type == TransactionType.VEHICLE_EXPENSE)
                && newExchangeRate != null
                && (oldExchangeRate == null || newExchangeRate.compareTo(oldExchangeRate) != 0)) {
            transaction.setExchangeRate(newExchangeRate);
            return true;
        }
        return false;
    }

    private boolean updateAmountIfNeeded(@NonNull Transaction transaction, BigDecimal newAmount, @NonNull BigDecimal oldAmount) {
        if (newAmount != null && newAmount.compareTo(oldAmount) != 0) {
            transaction.setAmount(newAmount);
            return true;
        }
        return false;
    }

    private boolean checkConvertedAmountChanged(@NonNull TransactionType type, BigDecimal newConvertedAmount, BigDecimal oldConvertedAmount) {
        return type == TransactionType.VEHICLE_EXPENSE
                && newConvertedAmount != null
                && (oldConvertedAmount == null || newConvertedAmount.compareTo(oldConvertedAmount) != 0);
    }
}
