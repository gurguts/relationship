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
import org.example.userservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTransactionService {
    private static final String ERROR_CODE_TRANSACTION_TYPE_REQUIRED = "TRANSACTION_TYPE_REQUIRED";
    private static final String ERROR_CODE_VEHICLE_EXPENSE_NOT_SUPPORTED = "VEHICLE_EXPENSE_NOT_SUPPORTED";
    private static final String ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE = "UNSUPPORTED_TRANSACTION_TYPE";
    private static final String ERROR_CODE_ACCESS_DENIED = "ACCESS_DENIED";

    private final TransactionRepository transactionRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final TransactionCreationService creationService;
    private final TransactionUpdateService updateService;
    private final TransactionDeletionService deletionService;
    private final TransactionValidationService validationService;

    @Transactional(readOnly = true)
    public Transaction getTransactionById(@NonNull Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByVehicleId(@NonNull Long vehicleId) {
        return transactionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Transaction>> getTransactionsByVehicleIds(@NonNull List<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Transaction> transactions = transactionRepository.findByVehicleIdInOrderByCreatedAtDesc(vehicleIds);
        return transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getVehicleId));
    }

    @Transactional
    public void deleteTransactionsByVehicleId(@NonNull Long vehicleId) {
        deletionService.deleteTransactionsByVehicleId(vehicleId);
    }

    @Transactional
    public Transaction createTransaction(@NonNull Transaction transaction) {
        Long executorUserId = SecurityUtils.getCurrentUserId();
        if (executorUserId == null) {
            throw new TransactionException(ERROR_CODE_ACCESS_DENIED, "Current user ID is required");
        }
        transaction.setExecutorUserId(executorUserId);

        if (transaction.getCategoryId() != null) {
            transactionCategoryRepository.findById(transaction.getCategoryId())
                    .orElseThrow(() -> new TransactionCategoryNotFoundException(
                            String.format("Transaction category with ID %d not found", transaction.getCategoryId())));
        }

        TransactionType type = transaction.getType();
        if (type == null) {
            throw new TransactionException(ERROR_CODE_TRANSACTION_TYPE_REQUIRED, "Transaction type is required");
        }

        validationService.checkAccountPermissions(executorUserId, transaction);

        return switch (type) {
            case INTERNAL_TRANSFER -> creationService.createInternalTransfer(transaction);
            case EXTERNAL_INCOME -> creationService.createExternalIncome(transaction);
            case EXTERNAL_EXPENSE -> creationService.createExternalExpense(transaction);
            case CLIENT_PAYMENT -> creationService.createClientPayment(transaction);
            case CURRENCY_CONVERSION -> creationService.createCurrencyConversion(transaction);
            case VEHICLE_EXPENSE -> throw new TransactionException(ERROR_CODE_VEHICLE_EXPENSE_NOT_SUPPORTED,
                    "Vehicle expenses should be created through the vehicle expense endpoint, not as financial transactions");
            default ->
                    throw new TransactionException(ERROR_CODE_UNSUPPORTED_TRANSACTION_TYPE, String.format("Unsupported transaction type: %s", type));
        };
    }

    @Transactional
    public Transaction updateTransaction(@NonNull Long transactionId, Long categoryId, String description, 
                                        BigDecimal newAmount, BigDecimal newExchangeRate, BigDecimal newCommission, 
                                        BigDecimal newConvertedAmount, Long counterpartyId) {
        return updateService.updateTransaction(transactionId, categoryId, description, newAmount, 
                newExchangeRate, newCommission, newConvertedAmount, counterpartyId);
    }

    @Transactional
    public void deleteTransaction(@NonNull Long transactionId) {
        deletionService.deleteTransaction(transactionId);
    }
}
