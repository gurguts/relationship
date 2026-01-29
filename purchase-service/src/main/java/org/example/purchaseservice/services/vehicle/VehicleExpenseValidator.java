package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.repositories.VehicleExpenseRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VehicleExpenseValidator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final VehicleRepository vehicleRepository;
    private final VehicleExpenseRepository vehicleExpenseRepository;

    public void validateVehicleExists(Long vehicleId) {
        if (vehicleId == null) {
            throw new PurchaseException("VEHICLE_ID_REQUIRED", "Vehicle ID is required");
        }
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new PurchaseException("VEHICLE_NOT_FOUND",
                    String.format("Vehicle not found: id=%d", vehicleId));
        }
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new PurchaseException("INVALID_AMOUNT", "Amount must be greater than zero");
        }
    }

    public String normalizeCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new PurchaseException("CURRENCY_REQUIRED", "Currency is required");
        }
        return currency.trim().toUpperCase();
    }

    public void validateNoDuplicateCategory(@NonNull Long vehicleId, Long categoryId) {
        if (categoryId != null) {
            if (vehicleExpenseRepository.existsByVehicleIdAndCategoryId(vehicleId, categoryId)) {
                throw new PurchaseException("DUPLICATE_CATEGORY_EXPENSE",
                        "Expense with this category already exists for this vehicle");
            }
        }
    }

    public void validateNoDuplicateCategoryOnUpdate(@NonNull Long vehicleId, Long categoryId, @NonNull Long expenseId) {
        if (categoryId != null) {
            if (vehicleExpenseRepository.existsByVehicleIdAndCategoryIdAndIdNot(vehicleId, categoryId, expenseId)) {
                throw new PurchaseException("DUPLICATE_CATEGORY_EXPENSE",
                        "Expense with this category already exists for this vehicle");
            }
        }
    }

    public void validateVehicleIds(@NonNull List<Long> vehicleIds) {
        if (vehicleIds.stream().anyMatch(Objects::isNull)) {
            throw new PurchaseException("INVALID_VEHICLE_ID", "Vehicle ID list cannot contain null values");
        }
    }
}
