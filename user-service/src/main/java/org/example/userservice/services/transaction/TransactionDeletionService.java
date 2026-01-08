package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.account.AccountBalanceService;
import org.example.userservice.clients.VehicleCostApiClient;
import org.example.userservice.models.dto.UpdateVehicleCostRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionDeletionService {
    private static final String ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE = "UNSUPPORTED_TRANSACTION_TYPE";
    private static final String ERROR_CODE_FAILED_TO_REVERT_VEHICLE_COST = "FAILED_TO_REVERT_VEHICLE_COST";
    private static final String OPERATION_SUBTRACT = "subtract";
    private static final String BALANCE_KEY_SEPARATOR = "_";

    private final TransactionRepository transactionRepository;
    private final AccountBalanceService accountBalanceService;
    private final VehicleCostApiClient vehicleCostApiClient;

    @Transactional
    public void deleteTransaction(@NonNull Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));

        revertTransactionEffects(transaction);

        transactionRepository.delete(transaction);
    }

    @Transactional
    public void deleteTransactionsByVehicleId(@NonNull Long vehicleId) {
        List<Transaction> transactions = transactionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        log.info("Deleting {} transactions for vehicleId: {}", transactions.size(), vehicleId);
        
        if (transactions.isEmpty()) {
            return;
        }
        
        Map<String, BigDecimal> balanceAdjustments = calculateBalanceAdjustmentsForVehicleDeletion(transactions);
        Map<Long, BigDecimal> vehicleCostAdjustments = calculateVehicleCostAdjustmentsForVehicleDeletion(transactions);
        
        applyBalanceAdjustments(balanceAdjustments);
        applyVehicleCostAdjustments(vehicleCostAdjustments);
        
        transactionRepository.deleteAll(transactions);
        
        log.info("Successfully deleted all transactions for vehicleId: {}", vehicleId);
    }

    private void revertTransactionEffects(@NonNull Transaction transaction) {
        TransactionType type = transaction.getType();
        BigDecimal amount = transaction.getAmount();
        BigDecimal convertedAmount = transaction.getConvertedAmount();

        switch (type) {
            case INTERNAL_TRANSFER:
                BigDecimal commission = transaction.getCommission();
                BigDecimal transferAmount = commission != null ? amount.subtract(commission) : amount;
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), amount);
                accountBalanceService.subtractAmount(transaction.getToAccountId(), transaction.getCurrency(), transferAmount);
                break;
            case EXTERNAL_INCOME:
                accountBalanceService.subtractAmount(transaction.getToAccountId(), transaction.getCurrency(), amount);
                break;
            case EXTERNAL_EXPENSE:
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), amount);
                break;
            case CLIENT_PAYMENT:
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), amount);
                break;
            case CURRENCY_CONVERSION:
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), amount);
                if (convertedAmount != null && transaction.getConvertedCurrency() != null) {
                    accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getConvertedCurrency(), convertedAmount);
                }
                break;
            case VEHICLE_EXPENSE:
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), amount);
                if (convertedAmount != null && convertedAmount.compareTo(BigDecimal.ZERO) > 0 && transaction.getVehicleId() != null) {
                    revertVehicleCost(transaction.getVehicleId(), convertedAmount);
                }
                break;
            default:
                throw new TransactionException(ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE, String.format("Unsupported transaction type for deletion: %s", type));
        }
    }

    private Map<String, BigDecimal> calculateBalanceAdjustmentsForVehicleDeletion(@NonNull List<Transaction> transactions) {
        Map<String, BigDecimal> balanceAdjustments = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            TransactionType type = transaction.getType();
            BigDecimal amount = transaction.getAmount();
            BigDecimal convertedAmount = transaction.getConvertedAmount();
            
            switch (type) {
                case INTERNAL_TRANSFER:
                    BigDecimal commission = transaction.getCommission();
                    BigDecimal transferAmount = commission != null ? amount.subtract(commission) : amount;
                    String fromKey = transaction.getFromAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    String toKey = transaction.getToAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    balanceAdjustments.merge(fromKey, amount, BigDecimal::add);
                    balanceAdjustments.merge(toKey, transferAmount.negate(), BigDecimal::add);
                    break;
                case EXTERNAL_INCOME:
                    String toAccountKey = transaction.getToAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    balanceAdjustments.merge(toAccountKey, amount.negate(), BigDecimal::add);
                    break;
                case EXTERNAL_EXPENSE:
                    String fromAccountKey = transaction.getFromAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    balanceAdjustments.merge(fromAccountKey, amount, BigDecimal::add);
                    break;
                case CLIENT_PAYMENT:
                    String clientPaymentKey = transaction.getFromAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    balanceAdjustments.merge(clientPaymentKey, amount, BigDecimal::add);
                    break;
                case CURRENCY_CONVERSION:
                    String currencyFromKey = transaction.getFromAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    balanceAdjustments.merge(currencyFromKey, amount, BigDecimal::add);
                    if (convertedAmount != null && transaction.getConvertedCurrency() != null) {
                        String currencyToKey = transaction.getFromAccountId() + BALANCE_KEY_SEPARATOR + transaction.getConvertedCurrency();
                        balanceAdjustments.merge(currencyToKey, convertedAmount.negate(), BigDecimal::add);
                    }
                    break;
                case VEHICLE_EXPENSE:
                    String vehicleExpenseKey = transaction.getFromAccountId() + BALANCE_KEY_SEPARATOR + transaction.getCurrency();
                    balanceAdjustments.merge(vehicleExpenseKey, amount, BigDecimal::add);
                    break;
                case DEPOSIT:
                case WITHDRAWAL:
                case PURCHASE:
                    log.warn("Unsupported transaction type {} for vehicle deletion, skipping balance adjustment", type);
                    break;
            }
        }
        
        return balanceAdjustments;
    }

    private Map<Long, BigDecimal> calculateVehicleCostAdjustmentsForVehicleDeletion(@NonNull List<Transaction> transactions) {
        Map<Long, BigDecimal> vehicleCostAdjustments = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.VEHICLE_EXPENSE) {
                BigDecimal convertedAmount = transaction.getConvertedAmount();
                if (convertedAmount != null && convertedAmount.compareTo(BigDecimal.ZERO) > 0 && transaction.getVehicleId() != null) {
                    vehicleCostAdjustments.merge(transaction.getVehicleId(), convertedAmount.negate(), BigDecimal::add);
                }
            }
        }
        
        return vehicleCostAdjustments;
    }

    private void applyBalanceAdjustments(@NonNull Map<String, BigDecimal> balanceAdjustments) {
        for (Map.Entry<String, BigDecimal> entry : balanceAdjustments.entrySet()) {
            String[] parts = entry.getKey().split(BALANCE_KEY_SEPARATOR);
            Long accountId = Long.parseLong(parts[0]);
            String currency = parts[1];
            BigDecimal adjustment = entry.getValue();
            
            if (adjustment.compareTo(BigDecimal.ZERO) > 0) {
                accountBalanceService.addAmount(accountId, currency, adjustment);
            } else if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
                accountBalanceService.subtractAmount(accountId, currency, adjustment.abs());
            }
        }
    }

    private void applyVehicleCostAdjustments(@NonNull Map<Long, BigDecimal> vehicleCostAdjustments) {
        for (Map.Entry<Long, BigDecimal> entry : vehicleCostAdjustments.entrySet()) {
            try {
                UpdateVehicleCostRequest request = new UpdateVehicleCostRequest();
                request.setAmountEur(entry.getValue().abs());
                request.setOperation(OPERATION_SUBTRACT);
                vehicleCostApiClient.updateVehicleCost(entry.getKey(), request);
            } catch (Exception e) {
                log.error("Failed to revert vehicle cost for vehicleId {}: {}", entry.getKey(), e.getMessage());
                throw new TransactionException(ERROR_CODE_FAILED_TO_REVERT_VEHICLE_COST, "Failed to revert vehicle cost: " + e.getMessage());
            }
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

