package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionCategoryNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.IAccountBalanceService;
import org.example.userservice.services.impl.ITransactionUpdateService;
import org.example.userservice.clients.VehicleCostApiClient;
import org.example.userservice.models.dto.UpdateVehicleCostRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionUpdateService implements ITransactionUpdateService {
    private static final String ERROR_CODE_INVALID_COMMISSION = "INVALID_COMMISSION";
    private static final String ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE = "UNSUPPORTED_TRANSACTION_TYPE";
    private static final String ERROR_CODE_FAILED_TO_UPDATE_VEHICLE_COST = "FAILED_TO_UPDATE_VEHICLE_COST";
    private static final String ERROR_CODE_FAILED_TO_REVERT_VEHICLE_COST = "FAILED_TO_REVERT_VEHICLE_COST";
    private static final String OPERATION_ADD = "add";
    private static final String OPERATION_SUBTRACT = "subtract";
    private static final int EXCHANGE_RATE_SCALE = 6;
    private static final int CONVERTED_AMOUNT_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final IAccountBalanceService accountBalanceService;
    private final VehicleCostApiClient vehicleCostApiClient;

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

        updateTransactionFields(transaction, categoryId, description, counterpartyId, type);

        boolean commissionChanged = updateCommissionIfNeeded(transaction, type, newCommission, newAmount, oldAmount, oldCommission);
        boolean exchangeRateChanged = updateExchangeRateIfNeeded(transaction, type, newExchangeRate, oldExchangeRate);
        boolean amountChanged = updateAmountIfNeeded(transaction, newAmount, oldAmount);
        boolean convertedAmountChanged = checkConvertedAmountChanged(type, newConvertedAmount, oldConvertedAmount);

        if (amountChanged || exchangeRateChanged || commissionChanged || convertedAmountChanged) {
            updateTransactionBalanceEffects(transaction, type, oldAmount, oldCommission, newAmount, newCommission, 
                    oldExchangeRate, newExchangeRate, oldConvertedAmount, newConvertedAmount, 
                    amountChanged, commissionChanged, exchangeRateChanged, convertedAmountChanged);
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction updated: id={}", saved.getId());
        return saved;
    }

    private void updateTransactionFields(@NonNull Transaction transaction, Long categoryId, String description, 
                                        Long counterpartyId, @NonNull TransactionType type) {
        if (categoryId != null) {
            transactionCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new TransactionCategoryNotFoundException(
                            String.format("Transaction category with ID %d not found", categoryId)));
            transaction.setCategoryId(categoryId);
        }

        if (description != null) {
            transaction.setDescription(description);
        }

        if (counterpartyId != null) {
            transaction.setCounterpartyId(counterpartyId);
        } else if (transaction.getCounterpartyId() != null) {
            if (type == TransactionType.EXTERNAL_INCOME || type == TransactionType.EXTERNAL_EXPENSE) {
                transaction.setCounterpartyId(null);
            }
        }
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

    private void updateTransactionBalanceEffects(@NonNull Transaction transaction, @NonNull TransactionType type,
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
            revertVehicleCost(transaction.getVehicleId(), oldConvertedAmount);
        }
        
        accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), newAmount);
        
        BigDecimal newConvertedAmountValue = calculateNewConvertedAmountForVehicleExpense(
                oldConvertedAmount, oldExchangeRate, newExchangeRate, newConvertedAmount,
                amountChanged, exchangeRateChanged, convertedAmountChanged, newAmount, oldAmount, transaction);
        
        if (newConvertedAmountValue != null && newConvertedAmountValue.compareTo(BigDecimal.ZERO) > 0) {
            updateVehicleCostAfterExpense(transaction.getVehicleId(), newConvertedAmountValue);
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

    private void updateVehicleCostAfterExpense(@NonNull Long vehicleId, @NonNull BigDecimal convertedAmount) {
        try {
            UpdateVehicleCostRequest request = new UpdateVehicleCostRequest();
            request.setAmountEur(convertedAmount);
            request.setOperation(OPERATION_ADD);
            vehicleCostApiClient.updateVehicleCost(vehicleId, request);
        } catch (Exception e) {
            log.error("Failed to update vehicle cost for vehicleId {}: {}", vehicleId, e.getMessage());
            throw new TransactionException(ERROR_CODE_FAILED_TO_UPDATE_VEHICLE_COST, "Failed to update vehicle cost: " + e.getMessage());
        }
    }

    private void revertVehicleCost(@NonNull Long vehicleId, @NonNull BigDecimal convertedAmount) {
        try {
            UpdateVehicleCostRequest request = new UpdateVehicleCostRequest();
            request.setAmountEur(convertedAmount);
            request.setOperation(OPERATION_SUBTRACT);
            vehicleCostApiClient.updateVehicleCost(vehicleId, request);
        } catch (Exception e) {
            log.error("Failed to revert vehicle cost for vehicleId {}: {}", vehicleId, e.getMessage());
            throw new TransactionException(ERROR_CODE_FAILED_TO_REVERT_VEHICLE_COST, "Failed to revert vehicle cost: " + e.getMessage());
        }
    }
}

