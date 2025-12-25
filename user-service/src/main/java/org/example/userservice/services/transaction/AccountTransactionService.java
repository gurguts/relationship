package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionCategoryNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.account.AccountBalanceService;
import org.example.userservice.services.branch.BranchPermissionService;
import org.example.userservice.clients.ExchangeRateApiClient;
import org.example.userservice.clients.VehicleCostApiClient;
import org.example.userservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountBalanceService accountBalanceService;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final BranchPermissionService branchPermissionService;
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final VehicleCostApiClient vehicleCostApiClient;

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByVehicleId(Long vehicleId) {
        return transactionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Transaction>> getTransactionsByVehicleIds(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Transaction> transactions = transactionRepository.findByVehicleIdInOrderByCreatedAtDesc(vehicleIds);
        return transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getVehicleId));
    }

    @Transactional
    public void deleteTransactionsByVehicleId(Long vehicleId) {
        List<Transaction> transactions = transactionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        log.info("Deleting {} transactions for vehicleId: {}", transactions.size(), vehicleId);
        
        if (transactions.isEmpty()) {
            return;
        }
        
        java.util.Map<String, java.math.BigDecimal> balanceAdjustments = new java.util.HashMap<>();
        java.util.Map<Long, java.math.BigDecimal> vehicleCostAdjustments = new java.util.HashMap<>();
        
        for (Transaction transaction : transactions) {
            TransactionType type = transaction.getType();
            BigDecimal amount = transaction.getAmount();
            BigDecimal convertedAmount = transaction.getConvertedAmount();
            
            switch (type) {
                case INTERNAL_TRANSFER:
                    BigDecimal commission = transaction.getCommission();
                    BigDecimal transferAmount = commission != null ? amount.subtract(commission) : amount;
                    String fromKey = transaction.getFromAccountId() + "_" + transaction.getCurrency();
                    String toKey = transaction.getToAccountId() + "_" + transaction.getCurrency();
                    balanceAdjustments.merge(fromKey, amount, BigDecimal::add);
                    balanceAdjustments.merge(toKey, transferAmount.negate(), BigDecimal::add);
                    break;
                case EXTERNAL_INCOME:
                    String toAccountKey = transaction.getToAccountId() + "_" + transaction.getCurrency();
                    balanceAdjustments.merge(toAccountKey, amount.negate(), BigDecimal::add);
                    break;
                case EXTERNAL_EXPENSE:
                    String fromAccountKey = transaction.getFromAccountId() + "_" + transaction.getCurrency();
                    balanceAdjustments.merge(fromAccountKey, amount, BigDecimal::add);
                    break;
                case CLIENT_PAYMENT:
                    String clientPaymentKey = transaction.getFromAccountId() + "_" + transaction.getCurrency();
                    balanceAdjustments.merge(clientPaymentKey, amount, BigDecimal::add);
                    break;
                case CURRENCY_CONVERSION:
                    String currencyFromKey = transaction.getFromAccountId() + "_" + transaction.getCurrency();
                    balanceAdjustments.merge(currencyFromKey, amount, BigDecimal::add);
                    if (convertedAmount != null && transaction.getConvertedCurrency() != null) {
                        String currencyToKey = transaction.getFromAccountId() + "_" + transaction.getConvertedCurrency();
                        balanceAdjustments.merge(currencyToKey, convertedAmount.negate(), BigDecimal::add);
                    }
                    break;
                case VEHICLE_EXPENSE:
                    String vehicleExpenseKey = transaction.getFromAccountId() + "_" + transaction.getCurrency();
                    balanceAdjustments.merge(vehicleExpenseKey, amount, BigDecimal::add);
                    if (convertedAmount != null && convertedAmount.compareTo(BigDecimal.ZERO) > 0 && transaction.getVehicleId() != null) {
                        vehicleCostAdjustments.merge(transaction.getVehicleId(), convertedAmount.negate(), BigDecimal::add);
                    }
                    break;
                case DEPOSIT:
                case WITHDRAWAL:
                case PURCHASE:
                    log.warn("Unsupported transaction type {} for vehicle deletion, skipping balance adjustment", type);
                    break;
            }
        }
        
        for (java.util.Map.Entry<String, java.math.BigDecimal> entry : balanceAdjustments.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long accountId = Long.parseLong(parts[0]);
            String currency = parts[1];
            BigDecimal adjustment = entry.getValue();
            
            if (adjustment.compareTo(BigDecimal.ZERO) > 0) {
                accountBalanceService.addAmount(accountId, currency, adjustment);
            } else if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
                accountBalanceService.subtractAmount(accountId, currency, adjustment.abs());
            }
        }
        
        for (java.util.Map.Entry<Long, java.math.BigDecimal> entry : vehicleCostAdjustments.entrySet()) {
            try {
                vehicleCostApiClient.updateVehicleCost(entry.getKey(), entry.getValue().abs(), "subtract");
            } catch (Exception e) {
                log.error("Failed to revert vehicle cost for vehicleId {}: {}", entry.getKey(), e.getMessage());
                throw new TransactionException("FAILED_TO_REVERT_VEHICLE_COST", "Failed to revert vehicle cost: " + e.getMessage());
            }
        }
        
        transactionRepository.deleteAll(transactions);
        
        log.info("Successfully deleted all transactions for vehicleId: {}", vehicleId);
    }

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        Long executorUserId = SecurityUtils.getCurrentUserId();
        transaction.setExecutorUserId(executorUserId);

        if (transaction.getCategoryId() != null) {
            transactionCategoryRepository.findById(transaction.getCategoryId())
                    .orElseThrow(() -> new TransactionCategoryNotFoundException(
                            String.format("Transaction category with ID %d not found", transaction.getCategoryId())));
        }

        TransactionType type = transaction.getType();
        if (type == null) {
            throw new TransactionException("TRANSACTION_TYPE_REQUIRED", "Transaction type is required");
        }

        checkAccountPermissions(executorUserId, transaction);

        switch (type) {
            case INTERNAL_TRANSFER:
                return createInternalTransfer(transaction);
            case EXTERNAL_INCOME:
                return createExternalIncome(transaction);
            case EXTERNAL_EXPENSE:
                return createExternalExpense(transaction);
            case CLIENT_PAYMENT:
                return createClientPayment(transaction);
            case CURRENCY_CONVERSION:
                return createCurrencyConversion(transaction);
            case VEHICLE_EXPENSE:
                return createVehicleExpense(transaction);
            default:
                throw new TransactionException("UNSUPPORTED_TRANSACTION_TYPE", String.format("Unsupported transaction type: %s", type));
        }
    }

    private void checkAccountPermissions(Long userId, Transaction transaction) {
        TransactionType type = transaction.getType();
        
        if (type == TransactionType.INTERNAL_TRANSFER || type == TransactionType.CURRENCY_CONVERSION) {
            if (transaction.getFromAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getFromAccountId());
            }
            if (transaction.getToAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getToAccountId());
            }
        } else if (type == TransactionType.EXTERNAL_INCOME) {

            if (transaction.getToAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getToAccountId());
            }
        } else if (type == TransactionType.EXTERNAL_EXPENSE) {

            if (transaction.getFromAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getFromAccountId());
            }
        } else if (type == TransactionType.CLIENT_PAYMENT) {
            if (transaction.getFromAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getFromAccountId());
            }
        } else if (type == TransactionType.VEHICLE_EXPENSE) {
            if (transaction.getFromAccountId() != null) {
                checkAccountOperatePermission(userId, transaction.getFromAccountId());
            }
        }
    }

    private void checkAccountOperatePermission(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account with ID %d not found", accountId)));

        if (account.getBranchId() == null) {
            return;
        }

        if (!branchPermissionService.canOperate(userId, account.getBranchId())) {
            throw new TransactionException("ACCESS_DENIED",
                    String.format("User does not have permission to operate on account %d (branch %d)", 
                            accountId, account.getBranchId()));
        }
    }

    @Transactional
    protected Transaction createInternalTransfer(Transaction transaction) {
        validateAccounts(transaction.getFromAccountId(), transaction.getToAccountId());
        validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());
        validateCurrency(transaction.getToAccountId(), transaction.getCurrency());

        if (transaction.getFromAccountId().equals(transaction.getToAccountId())) {
            throw new TransactionException("SAME_ACCOUNTS", "Source and destination accounts cannot be the same");
        }

        BigDecimal amount = transaction.getAmount();
        BigDecimal commission = transaction.getCommission();
        
        // If commission is provided, validate it
        if (commission != null) {
            if (commission.compareTo(BigDecimal.ZERO) < 0) {
                throw new TransactionException("INVALID_COMMISSION", "Commission cannot be negative");
            }
            if (commission.compareTo(amount) >= 0) {
                throw new TransactionException("INVALID_COMMISSION", "Commission cannot be greater than or equal to the transfer amount");
            }
        }

        // Calculate amount to transfer (amount minus commission if commission exists)
        BigDecimal transferAmount = commission != null ? amount.subtract(commission) : amount;

        // Subtract full amount from source account (including commission)
        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                transaction.getCurrency(),
                amount
        );

        // Add transfer amount (amount minus commission) to destination account
        accountBalanceService.addAmount(
                transaction.getToAccountId(),
                transaction.getCurrency(),
                transferAmount
        );

        return transactionRepository.save(transaction);
    }

    @Transactional
    protected Transaction createExternalIncome(Transaction transaction) {
        validateAccount(transaction.getToAccountId());
        validateCurrency(transaction.getToAccountId(), transaction.getCurrency());

        // Add money to account
        accountBalanceService.addAmount(
                transaction.getToAccountId(),
                transaction.getCurrency(),
                transaction.getAmount()
        );

        return transactionRepository.save(transaction);
    }

    @Transactional
    protected Transaction createExternalExpense(Transaction transaction) {
        validateAccount(transaction.getFromAccountId());
        validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());

        // Subtract money from account
        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                transaction.getCurrency(),
                transaction.getAmount()
        );

        return transactionRepository.save(transaction);
    }

    @Transactional
    protected Transaction createVehicleExpense(Transaction transaction) {
        validateAccount(transaction.getFromAccountId());
        validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());

        if (transaction.getVehicleId() == null) {
            throw new TransactionException("VEHICLE_ID_REQUIRED", "Vehicle ID is required for vehicle expense");
        }

        String currency = transaction.getCurrency();
        BigDecimal amount = transaction.getAmount();
        BigDecimal exchangeRate = transaction.getExchangeRate();
        BigDecimal convertedAmount = transaction.getConvertedAmount();

        if (currency == null || currency.trim().isEmpty()) {
            throw new TransactionException("CURRENCY_REQUIRED", "Currency is required for vehicle expense");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("INVALID_AMOUNT", "Amount must be greater than zero");
        }

        if (!"EUR".equalsIgnoreCase(currency)) {
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                try {
                    exchangeRate = exchangeRateApiClient.getExchangeRateToEur(currency);
                    if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new TransactionException("EXCHANGE_RATE_NOT_FOUND", "Exchange rate not found for currency: " + currency);
                    }
                } catch (Exception e) {
                    log.error("Failed to get exchange rate for currency {}: {}", currency, e.getMessage());
                    throw new TransactionException("FAILED_TO_GET_EXCHANGE_RATE", "Failed to get exchange rate for currency: " + currency);
                }
            }
            if (convertedAmount == null || convertedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                convertedAmount = amount.divide(exchangeRate, 6, RoundingMode.HALF_UP);
            } else {
                BigDecimal calculatedRate = amount.divide(convertedAmount, 6, RoundingMode.HALF_UP);
                exchangeRate = calculatedRate;
            }
        } else {
            exchangeRate = BigDecimal.ONE;
            if (convertedAmount == null || convertedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                convertedAmount = amount;
            } else if (convertedAmount.compareTo(amount) != 0) {
                throw new TransactionException("INVALID_EUR_CONVERSION", "For EUR currency, converted amount must equal amount");
            }
        }

        transaction.setExchangeRate(exchangeRate);
        transaction.setConvertedAmount(convertedAmount);
        transaction.setConvertedCurrency("EUR");
        transaction.setToAccountId(null);

        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                currency,
                amount
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            vehicleCostApiClient.updateVehicleCost(transaction.getVehicleId(), convertedAmount, "add");
        } catch (Exception e) {
            log.error("Failed to update vehicle cost for vehicleId {}: {}", transaction.getVehicleId(), e.getMessage());
            throw new TransactionException("FAILED_TO_UPDATE_VEHICLE_COST", "Failed to update vehicle cost: " + e.getMessage());
        }

        return savedTransaction;
    }

    @Transactional
    protected Transaction createClientPayment(Transaction transaction) {
        validateAccount(transaction.getFromAccountId());
        validateCurrency(transaction.getFromAccountId(), transaction.getCurrency());

        if (transaction.getClientId() == null) {
            throw new TransactionException("CLIENT_ID_REQUIRED", "Client ID is required for client payment");
        }

        // Subtract money from account (payment to client, no account credit)
        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                transaction.getCurrency(),
                transaction.getAmount()
        );

        // Set toAccountId to null for client payments
        transaction.setToAccountId(null);

        return transactionRepository.save(transaction);
    }

    @Transactional
    protected Transaction createCurrencyConversion(Transaction transaction) {
        validateAccount(transaction.getFromAccountId());
        
        if (transaction.getFromAccountId() == null || !transaction.getFromAccountId().equals(transaction.getToAccountId())) {
            throw new TransactionException("INVALID_CURRENCY_CONVERSION", "Currency conversion must be within the same account");
        }

        String fromCurrency = transaction.getCurrency();
        String toCurrency = transaction.getConvertedCurrency();
        
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            throw new TransactionException("SAME_CURRENCIES", "Source and destination currencies must be different");
        }

        validateCurrency(transaction.getFromAccountId(), fromCurrency);
        validateCurrency(transaction.getFromAccountId(), toCurrency);

        BigDecimal convertedAmount = transaction.getConvertedAmount();
        BigDecimal exchangeRate = transaction.getExchangeRate();
        
        // If convertedAmount is provided, calculate exchangeRate from it
        // Otherwise, if exchangeRate is provided, calculate convertedAmount from it
        if (convertedAmount != null && convertedAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionException("INVALID_AMOUNT", "Amount must be greater than zero");
            }
            exchangeRate = transaction.getAmount().divide(convertedAmount, 6, RoundingMode.HALF_UP);
            transaction.setExchangeRate(exchangeRate);
        } else if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            convertedAmount = transaction.getAmount()
                    .divide(exchangeRate, 2, RoundingMode.HALF_UP);
            transaction.setConvertedAmount(convertedAmount);
        } else {
            throw new TransactionException("EXCHANGE_RATE_OR_AMOUNT_REQUIRED", "Either exchange rate or converted amount must be provided");
        }

        // Subtract from source currency
        accountBalanceService.subtractAmount(
                transaction.getFromAccountId(),
                fromCurrency,
                transaction.getAmount()
        );

        // Add to destination currency
        accountBalanceService.addAmount(
                transaction.getFromAccountId(),
                toCurrency,
                convertedAmount
        );

        return transactionRepository.save(transaction);
    }

    private void validateAccount(Long accountId) {
        if (accountId == null) {
            throw new TransactionException("ACCOUNT_ID_REQUIRED", "Account ID is required");
        }
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account with ID %d not found", accountId)));
    }

    private void validateAccounts(Long fromAccountId, Long toAccountId) {
        validateAccount(fromAccountId);
        validateAccount(toAccountId);
    }

    private void validateCurrency(Long accountId, String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new TransactionException("CURRENCY_REQUIRED", "Currency is required");
        }
        try {
            accountBalanceService.getBalance(accountId, currency);
        } catch (AccountNotFoundException e) {
            throw new AccountException("CURRENCY",
                    String.format("Currency %s is not supported for account %d", currency, accountId));
        }
    }

    @Transactional
    public Transaction updateTransaction(Long transactionId, Long categoryId, String description, BigDecimal newAmount, BigDecimal newExchangeRate, BigDecimal newCommission, BigDecimal newConvertedAmount, Long counterpartyId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));

        BigDecimal oldAmount = transaction.getAmount();
        BigDecimal oldCommission = transaction.getCommission();
        BigDecimal oldExchangeRate = transaction.getExchangeRate();
        BigDecimal oldConvertedAmount = transaction.getConvertedAmount();
        TransactionType type = transaction.getType();

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
        } else if (counterpartyId == null && transaction.getCounterpartyId() != null) {
            if (type == TransactionType.EXTERNAL_INCOME || type == TransactionType.EXTERNAL_EXPENSE) {
                transaction.setCounterpartyId(null);
            }
        }

        // Update commission if provided (only for internal transfer)
        boolean commissionChanged = false;
        if (type == TransactionType.INTERNAL_TRANSFER && newCommission != null) {
            if (newCommission.compareTo(BigDecimal.ZERO) < 0) {
                throw new TransactionException("INVALID_COMMISSION", "Commission cannot be negative");
            }
            BigDecimal currentAmount = newAmount != null ? newAmount : oldAmount;
            if (newCommission.compareTo(currentAmount) >= 0) {
                throw new TransactionException("INVALID_COMMISSION", "Commission cannot be greater than or equal to the transfer amount");
            }
            if (oldCommission == null || newCommission.compareTo(oldCommission) != 0) {
                transaction.setCommission(newCommission);
                commissionChanged = true;
            }
        } else if (type == TransactionType.INTERNAL_TRANSFER && newCommission == null && oldCommission != null) {
            // Allow removing commission by setting it to null
            transaction.setCommission(null);
            commissionChanged = true;
        }

        // Update exchange rate if provided (for currency conversion or vehicle expense)
        boolean exchangeRateChanged = false;
        if (type == TransactionType.CURRENCY_CONVERSION && newExchangeRate != null && 
            (oldExchangeRate == null || newExchangeRate.compareTo(oldExchangeRate) != 0)) {
            transaction.setExchangeRate(newExchangeRate);
            exchangeRateChanged = true;
        } else if (type == TransactionType.VEHICLE_EXPENSE && newExchangeRate != null && 
            (oldExchangeRate == null || newExchangeRate.compareTo(oldExchangeRate) != 0)) {
            transaction.setExchangeRate(newExchangeRate);
            exchangeRateChanged = true;
        }

        // Update amount if provided and different
        boolean amountChanged = newAmount != null && newAmount.compareTo(oldAmount) != 0;
        if (amountChanged) {
            transaction.setAmount(newAmount);
        }

        // Check if convertedAmount changed (for VEHICLE_EXPENSE)
        boolean convertedAmountChanged = false;
        if (type == TransactionType.VEHICLE_EXPENSE && newConvertedAmount != null && 
            (oldConvertedAmount == null || newConvertedAmount.compareTo(oldConvertedAmount) != 0)) {
            convertedAmountChanged = true;
        }

        // Revert old transaction effect and apply new amount/exchange rate/commission
        if (amountChanged || exchangeRateChanged || commissionChanged || convertedAmountChanged) {
            if (type == TransactionType.INTERNAL_TRANSFER) {
                // Calculate old transfer amount (amount minus commission)
                BigDecimal oldTransferAmount = oldCommission != null ? oldAmount.subtract(oldCommission) : oldAmount;
                
                // Revert: add full amount to from, subtract transfer amount from to
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
                accountBalanceService.subtractAmount(transaction.getToAccountId(), transaction.getCurrency(), oldTransferAmount);
                
                // Calculate new values
                BigDecimal currentAmount = amountChanged ? newAmount : oldAmount;
                BigDecimal currentCommission = commissionChanged ? newCommission : oldCommission;
                BigDecimal newTransferAmount = currentCommission != null ? currentAmount.subtract(currentCommission) : currentAmount;
                
                // Apply new: subtract full amount from from, add transfer amount to to
                accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), currentAmount);
                accountBalanceService.addAmount(transaction.getToAccountId(), transaction.getCurrency(), newTransferAmount);
            } else if (type == TransactionType.EXTERNAL_INCOME) {
                // Revert: subtract from to
                accountBalanceService.subtractAmount(transaction.getToAccountId(), transaction.getCurrency(), oldAmount);
                // Apply new: add to to
                accountBalanceService.addAmount(transaction.getToAccountId(), transaction.getCurrency(), newAmount);
            } else if (type == TransactionType.EXTERNAL_EXPENSE) {
                // Revert: add to from
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
                // Apply new: subtract from from
                accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), newAmount);
            } else if (type == TransactionType.CLIENT_PAYMENT) {
                // Revert: add to from
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
                // Apply new: subtract from from
                accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), newAmount);
            } else if (type == TransactionType.VEHICLE_EXPENSE) {
                // Revert: add to from account
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
                // Revert: subtract from vehicle cost (using old converted amount in EUR)
                if (oldConvertedAmount != null && oldConvertedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        vehicleCostApiClient.updateVehicleCost(transaction.getVehicleId(), oldConvertedAmount, "subtract");
                    } catch (Exception e) {
                        log.error("Failed to revert vehicle cost for vehicleId {}: {}", transaction.getVehicleId(), e.getMessage());
                        throw new TransactionException("FAILED_TO_REVERT_VEHICLE_COST", "Failed to revert vehicle cost: " + e.getMessage());
                    }
                }
                
                // Apply new: subtract from from account (in original currency)
                accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), newAmount);
                
                // Calculate new converted amount in EUR based on changes
                BigDecimal newConvertedAmountValue = oldConvertedAmount;
                BigDecimal finalExchangeRate = oldExchangeRate;
                
                BigDecimal currentAmount = amountChanged ? newAmount : oldAmount;
                
                if (newConvertedAmount != null && newConvertedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    newConvertedAmountValue = newConvertedAmount;
                    if (currentAmount != null && currentAmount.compareTo(BigDecimal.ZERO) > 0) {
                        finalExchangeRate = currentAmount.divide(newConvertedAmountValue, 6, RoundingMode.HALF_UP);
                    }
                    transaction.setConvertedAmount(newConvertedAmountValue);
                    transaction.setExchangeRate(finalExchangeRate);
                } else if (amountChanged && !exchangeRateChanged) {
                    if (oldExchangeRate != null && oldExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                        newConvertedAmountValue = newAmount.divide(oldExchangeRate, 6, RoundingMode.HALF_UP);
                        transaction.setConvertedAmount(newConvertedAmountValue);
                    }
                } else if (exchangeRateChanged && !amountChanged) {
                    if (newExchangeRate != null && newExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                        newConvertedAmountValue = oldAmount.divide(newExchangeRate, 6, RoundingMode.HALF_UP);
                        transaction.setConvertedAmount(newConvertedAmountValue);
                        transaction.setExchangeRate(newExchangeRate);
                    }
                } else if (amountChanged && exchangeRateChanged) {
                    if (newExchangeRate != null && newExchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                        newConvertedAmountValue = newAmount.divide(newExchangeRate, 6, RoundingMode.HALF_UP);
                        transaction.setConvertedAmount(newConvertedAmountValue);
                        transaction.setExchangeRate(newExchangeRate);
                    }
                }
                
                // Apply new: add to vehicle cost (in EUR)
                if (newConvertedAmountValue != null && newConvertedAmountValue.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        vehicleCostApiClient.updateVehicleCost(transaction.getVehicleId(), newConvertedAmountValue, "add");
                    } catch (Exception e) {
                        log.error("Failed to update vehicle cost for vehicleId {}: {}", transaction.getVehicleId(), e.getMessage());
                        throw new TransactionException("FAILED_TO_UPDATE_VEHICLE_COST", "Failed to update vehicle cost: " + e.getMessage());
                    }
                }
            } else if (type == TransactionType.CURRENCY_CONVERSION) {
                // Revert: add to from currency, subtract from to currency
                accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getCurrency(), oldAmount);
                if (transaction.getConvertedAmount() != null) {
                    accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getConvertedCurrency(), transaction.getConvertedAmount());
                }
                // Apply new: recalculate converted amount and apply
                BigDecimal currentAmount = amountChanged ? newAmount : oldAmount;
                BigDecimal currentExchangeRate = exchangeRateChanged ? newExchangeRate : oldExchangeRate;
                if (currentExchangeRate != null) {
                    BigDecimal calculatedConvertedAmount = currentAmount.divide(currentExchangeRate, 2, RoundingMode.HALF_UP);
                    transaction.setConvertedAmount(calculatedConvertedAmount);
                    accountBalanceService.subtractAmount(transaction.getFromAccountId(), transaction.getCurrency(), currentAmount);
                    accountBalanceService.addAmount(transaction.getFromAccountId(), transaction.getConvertedCurrency(), calculatedConvertedAmount);
                }
            } else {
                throw new TransactionException("UNSUPPORTED_TRANSACTION_TYPE", String.format("Unsupported transaction type for update: %s", type));
            }
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));

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
                    try {
                        vehicleCostApiClient.updateVehicleCost(transaction.getVehicleId(), convertedAmount, "subtract");
                    } catch (Exception e) {
                        log.error("Failed to revert vehicle cost for vehicleId {}: {}", transaction.getVehicleId(), e.getMessage());
                        throw new TransactionException("FAILED_TO_REVERT_VEHICLE_COST", "Failed to revert vehicle cost: " + e.getMessage());
                    }
                }
                break;
            default:
                throw new TransactionException("UNSUPPORTED_TRANSACTION_TYPE", String.format("Unsupported transaction type for deletion: %s", type));
        }

        transactionRepository.delete(transaction);
    }
}

