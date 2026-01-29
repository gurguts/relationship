package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.services.impl.IAccountBalanceService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class TransactionUpdateBalanceEffectsHandler {

    private static final String ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE = "UNSUPPORTED_TRANSACTION_TYPE";
    private static final int EXCHANGE_RATE_SCALE = 6;
    private static final int CONVERTED_AMOUNT_SCALE = 2;

    private final IAccountBalanceService accountBalanceService;
    private final VehicleCostUpdateHelper vehicleCostUpdateHelper;

    public void applyBalanceEffects(@NonNull Transaction transaction, @NonNull TransactionType type,
                                    @NonNull BigDecimal oldAmount, BigDecimal oldCommission,
                                    BigDecimal newAmount, BigDecimal newCommission,
                                    BigDecimal oldExchangeRate, BigDecimal newExchangeRate,
                                    BigDecimal oldConvertedAmount, BigDecimal newConvertedAmount,
                                    boolean amountChanged, boolean commissionChanged,
                                    boolean exchangeRateChanged, boolean convertedAmountChanged) {
        switch (type) {
            case INTERNAL_TRANSFER:
                updateInternalTransfer(transaction, oldAmount, oldCommission, newAmount, newCommission,
                        amountChanged, commissionChanged);
                break;
            case EXTERNAL_INCOME:
                updateExternalIncome(transaction, oldAmount, newAmount);
                break;
            case EXTERNAL_EXPENSE:
            case CLIENT_PAYMENT:
                updateExternalExpenseOrClientPayment(transaction, oldAmount, newAmount);
                break;
            case VEHICLE_EXPENSE:
                updateVehicleExpense(transaction, oldAmount, newAmount, oldExchangeRate, newExchangeRate,
                        oldConvertedAmount, newConvertedAmount, amountChanged, exchangeRateChanged, convertedAmountChanged);
                break;
            case CURRENCY_CONVERSION:
                updateCurrencyConversion(transaction, oldAmount, newAmount, oldExchangeRate, newExchangeRate,
                        amountChanged, exchangeRateChanged);
                break;
            default:
                throw new TransactionException(ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE,
                        String.format("Unsupported transaction type for update: %s", type));
        }
    }

    private void updateInternalTransfer(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount,
                                        BigDecimal oldCommission, BigDecimal newAmount, BigDecimal newCommission,
                                        boolean amountChanged, boolean commissionChanged) {
        BigDecimal oldTransferAmount = oldCommission != null ? oldAmount.subtract(oldCommission) : oldAmount;

        accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
        accountBalanceService.subtractAmount(transaction.getToAccountId(), transaction.getCurrency(), oldTransferAmount);

        BigDecimal currentAmount = amountChanged ? newAmount : oldAmount;
        BigDecimal currentCommission = commissionChanged ? newCommission : oldCommission;
        BigDecimal newTransferAmount = currentCommission != null ? currentAmount.subtract(currentCommission) : currentAmount;

        accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), currentAmount);
        accountBalanceService.addAmount(transaction.getToAccountId(), transaction.getCurrency(), newTransferAmount);
    }

    private void updateExternalIncome(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount, @NonNull BigDecimal newAmount) {
        accountBalanceService.subtractAmount(transaction.getToAccountId(), transaction.getCurrency(), oldAmount);
        accountBalanceService.addAmount(transaction.getToAccountId(), transaction.getCurrency(), newAmount);
    }

    private void updateExternalExpenseOrClientPayment(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount, @NonNull BigDecimal newAmount) {
        accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
        accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), newAmount);
    }

    private void updateVehicleExpense(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount, @NonNull BigDecimal newAmount,
                                     BigDecimal oldExchangeRate, BigDecimal newExchangeRate,
                                     BigDecimal oldConvertedAmount, BigDecimal newConvertedAmount,
                                     boolean amountChanged, boolean exchangeRateChanged, boolean convertedAmountChanged) {
        accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);

        if (oldConvertedAmount != null && oldConvertedAmount.compareTo(BigDecimal.ZERO) > 0) {
            vehicleCostUpdateHelper.subtractVehicleCost(transaction.getVehicleId(), oldConvertedAmount);
        }

        accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), newAmount);

        BigDecimal newConvertedAmountValue = calculateNewConvertedAmountForVehicleExpense(
                oldConvertedAmount, oldExchangeRate, newExchangeRate, newConvertedAmount,
                amountChanged, exchangeRateChanged, convertedAmountChanged, newAmount, oldAmount, transaction);

        if (newConvertedAmountValue != null && newConvertedAmountValue.compareTo(BigDecimal.ZERO) > 0) {
            vehicleCostUpdateHelper.addVehicleCost(transaction.getVehicleId(), newConvertedAmountValue);
        }
    }

    private BigDecimal calculateNewConvertedAmountForVehicleExpense(BigDecimal oldConvertedAmount, BigDecimal oldExchangeRate,
                                                                    BigDecimal newExchangeRate, BigDecimal newConvertedAmount,
                                                                    boolean amountChanged, boolean exchangeRateChanged, boolean convertedAmountChanged,
                                                                    @NonNull BigDecimal newAmount, @NonNull BigDecimal oldAmount,
                                                                    @NonNull Transaction transaction) {
        BigDecimal newConvertedAmountValue = oldConvertedAmount;
        BigDecimal finalExchangeRate = oldExchangeRate;
        BigDecimal currentAmount = amountChanged ? newAmount : oldAmount;

        if (convertedAmountChanged && newConvertedAmount != null && newConvertedAmount.compareTo(BigDecimal.ZERO) > 0) {
            newConvertedAmountValue = newConvertedAmount;
            if (currentAmount.compareTo(BigDecimal.ZERO) > 0) {
                finalExchangeRate = currentAmount.divide(newConvertedAmountValue, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            }
            transaction.setConvertedAmount(newConvertedAmountValue);
            transaction.setExchangeRate(finalExchangeRate);
        } else if (amountChanged && !exchangeRateChanged && !convertedAmountChanged) {
            if (oldExchangeRate != null && oldExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                newConvertedAmountValue = newAmount.divide(oldExchangeRate, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
                transaction.setConvertedAmount(newConvertedAmountValue);
            }
        } else if (exchangeRateChanged && !amountChanged && !convertedAmountChanged) {
            if (newExchangeRate != null && newExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                newConvertedAmountValue = oldAmount.divide(newExchangeRate, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
                transaction.setConvertedAmount(newConvertedAmountValue);
                transaction.setExchangeRate(newExchangeRate);
            }
        } else if (amountChanged && exchangeRateChanged && !convertedAmountChanged) {
            if (newExchangeRate != null && newExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                newConvertedAmountValue = newAmount.divide(newExchangeRate, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
                transaction.setConvertedAmount(newConvertedAmountValue);
                transaction.setExchangeRate(newExchangeRate);
            }
        }

        return newConvertedAmountValue;
    }

    private void updateCurrencyConversion(@NonNull Transaction transaction, @NonNull BigDecimal oldAmount, @NonNull BigDecimal newAmount,
                                         BigDecimal oldExchangeRate, BigDecimal newExchangeRate,
                                         boolean amountChanged, boolean exchangeRateChanged) {
        accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
        if (transaction.getConvertedAmount() != null) {
            accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getConvertedCurrency(), transaction.getConvertedAmount());
        }

        BigDecimal currentAmount = amountChanged ? newAmount : oldAmount;
        BigDecimal currentExchangeRate = exchangeRateChanged ? newExchangeRate : oldExchangeRate;
        if (currentExchangeRate != null) {
            BigDecimal calculatedConvertedAmount = currentAmount.divide(currentExchangeRate, CONVERTED_AMOUNT_SCALE, RoundingMode.HALF_UP);
            transaction.setConvertedAmount(calculatedConvertedAmount);
            accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), currentAmount);
            accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getConvertedCurrency(), calculatedConvertedAmount);
        }
    }
}
