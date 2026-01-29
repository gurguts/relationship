package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.IAccountBalanceService;
import org.example.userservice.services.impl.ITransactionDeletionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionDeletionService implements ITransactionDeletionService {
    private static final String BALANCE_KEY_SEPARATOR = "_";

    private final TransactionRepository transactionRepository;
    private final IAccountBalanceService accountBalanceService;
    private final VehicleCostUpdateHelper vehicleCostUpdateHelper;

    @Override
    @Transactional
    public void deleteTransaction(@NonNull Long transactionId) {
        log.info("Deleting transaction: id={}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException(
                        String.format("Transaction with ID %d not found", transactionId)));

        TransactionRevertCalculator.RevertPlan plan = TransactionRevertCalculator.computeRevertPlan(transaction);
        applyPlan(plan);

        transactionRepository.delete(transaction);
        log.info("Transaction deleted: id={}", transactionId);
    }

    @Override
    @Transactional
    public void deleteTransactionsByVehicleId(@NonNull Long vehicleId) {
        List<Transaction> transactions = transactionRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        log.info("Deleting {} transactions for vehicleId: {}", transactions.size(), vehicleId);

        if (transactions.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> balanceAdjustments = new HashMap<>();
        Map<Long, BigDecimal> vehicleCostAdjustments = new HashMap<>();

        for (Transaction transaction : transactions) {
            TransactionRevertCalculator.RevertPlan plan = TransactionRevertCalculator.computeRevertPlan(transaction);
            for (TransactionRevertCalculator.BalanceAdjustment a : plan.balanceAdjustments()) {
                String key = a.accountId() + BALANCE_KEY_SEPARATOR + a.currency();
                balanceAdjustments.merge(key, a.amount(), BigDecimal::add);
            }
            for (TransactionRevertCalculator.VehicleCostRevert v : plan.vehicleCostReverts()) {
                vehicleCostAdjustments.merge(v.vehicleId(), v.amountToSubtract().negate(), BigDecimal::add);
            }
        }

        applyBalanceAdjustments(balanceAdjustments);
        applyVehicleCostAdjustments(vehicleCostAdjustments);

        transactionRepository.deleteAll(transactions);

        log.info("Successfully deleted all transactions for vehicleId: {}", vehicleId);
    }

    private void applyPlan(TransactionRevertCalculator.RevertPlan plan) {
        for (TransactionRevertCalculator.BalanceAdjustment a : plan.balanceAdjustments()) {
            if (a.amount().compareTo(BigDecimal.ZERO) > 0) {
                accountBalanceService.addAmount(a.accountId(), a.currency(), a.amount());
            } else if (a.amount().compareTo(BigDecimal.ZERO) < 0) {
                accountBalanceService.subtractAmount(a.accountId(), a.currency(), a.amount().abs());
            }
        }
        for (TransactionRevertCalculator.VehicleCostRevert v : plan.vehicleCostReverts()) {
            vehicleCostUpdateHelper.subtractVehicleCost(v.vehicleId(), v.amountToSubtract());
        }
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
            vehicleCostUpdateHelper.subtractVehicleCost(entry.getKey(), entry.getValue().abs());
        }
    }
}

