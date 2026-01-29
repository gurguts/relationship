package org.example.userservice.services.transaction;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TransactionRevertCalculator {

    private TransactionRevertCalculator() {
    }

    public static RevertPlan computeRevertPlan(@NonNull Transaction transaction) {
        TransactionType type = transaction.getType();
        BigDecimal amount = transaction.getAmount();
        BigDecimal convertedAmount = transaction.getConvertedAmount();

        return switch (type) {
            case INTERNAL_TRANSFER -> internalTransferPlan(transaction, amount);
            case EXTERNAL_INCOME -> externalIncomePlan(transaction, amount);
            case EXTERNAL_EXPENSE -> externalExpensePlan(transaction, amount);
            case CLIENT_PAYMENT -> clientPaymentPlan(transaction, amount);
            case CURRENCY_CONVERSION -> currencyConversionPlan(transaction, amount, convertedAmount);
            case VEHICLE_EXPENSE -> vehicleExpensePlan(transaction, amount, convertedAmount);
            case DEPOSIT, WITHDRAWAL, PURCHASE -> new RevertPlan(List.of(), List.of());
        };
    }

    private static RevertPlan internalTransferPlan(Transaction t, BigDecimal amount) {
        BigDecimal commission = t.getCommission();
        BigDecimal transferAmount = commission != null ? amount.subtract(commission) : amount;
        List<BalanceAdjustment> balance = List.of(
                new BalanceAdjustment(t.getFromAccountId(), t.getCurrency(), amount),
                new BalanceAdjustment(t.getToAccountId(), t.getCurrency(), transferAmount.negate())
        );
        return new RevertPlan(balance, List.of());
    }

    private static RevertPlan externalIncomePlan(Transaction t, BigDecimal amount) {
        return new RevertPlan(
                List.of(new BalanceAdjustment(t.getToAccountId(), t.getCurrency(), amount.negate())),
                List.of()
        );
    }

    private static RevertPlan externalExpensePlan(Transaction t, BigDecimal amount) {
        return new RevertPlan(
                List.of(new BalanceAdjustment(t.getFromAccountId(), t.getCurrency(), amount)),
                List.of()
        );
    }

    private static RevertPlan clientPaymentPlan(Transaction t, BigDecimal amount) {
        return new RevertPlan(
                List.of(new BalanceAdjustment(t.getFromAccountId(), t.getCurrency(), amount)),
                List.of()
        );
    }

    private static RevertPlan currencyConversionPlan(Transaction t, BigDecimal amount, BigDecimal convertedAmount) {
        List<BalanceAdjustment> balance = new ArrayList<>();
        balance.add(new BalanceAdjustment(t.getFromAccountId(), t.getCurrency(), amount));
        if (convertedAmount != null && t.getConvertedCurrency() != null) {
            balance.add(new BalanceAdjustment(t.getFromAccountId(), t.getConvertedCurrency(), convertedAmount.negate()));
        }
        return new RevertPlan(balance, List.of());
    }

    private static RevertPlan vehicleExpensePlan(Transaction t, BigDecimal amount, BigDecimal convertedAmount) {
        List<BalanceAdjustment> balance = List.of(new BalanceAdjustment(t.getFromAccountId(), t.getCurrency(), amount));
        List<VehicleCostRevert> vehicle = new ArrayList<>();
        if (convertedAmount != null && convertedAmount.compareTo(BigDecimal.ZERO) > 0 && t.getVehicleId() != null) {
            vehicle.add(new VehicleCostRevert(t.getVehicleId(), convertedAmount));
        }
        return new RevertPlan(balance, vehicle);
    }

    public record BalanceAdjustment(long accountId, String currency, BigDecimal amount) {
    }

    public record VehicleCostRevert(long vehicleId, BigDecimal amountToSubtract) {
    }

    public record RevertPlan(List<BalanceAdjustment> balanceAdjustments, List<VehicleCostRevert> vehicleCostReverts) {
        public RevertPlan {
            balanceAdjustments = balanceAdjustments != null ? List.copyOf(balanceAdjustments) : Collections.emptyList();
            vehicleCostReverts = vehicleCostReverts != null ? List.copyOf(vehicleCostReverts) : Collections.emptyList();
        }
    }
}
