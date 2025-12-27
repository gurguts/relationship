package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseUpdateDTO;
import org.example.purchaseservice.repositories.VehicleExpenseRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.services.ExchangeRateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpenseService {
    
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleRepository vehicleRepository;
    private final ExchangeRateService exchangeRateService;
    
    @Transactional
    public VehicleExpense createVehicleExpense(VehicleExpenseCreateDTO dto) {
        if (dto.getVehicleId() == null) {
            throw new PurchaseException("VEHICLE_ID_REQUIRED", "Vehicle ID is required");
        }
        
        // Validate vehicle exists
        if (!vehicleRepository.existsById(dto.getVehicleId())) {
            throw new PurchaseException("VEHICLE_NOT_FOUND",
                    String.format("Vehicle not found: id=%d", dto.getVehicleId()));
        }
        
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_AMOUNT", "Amount must be greater than zero");
        }
        
        // Check if expense with the same category already exists for this vehicle
        if (dto.getCategoryId() != null) {
            if (vehicleExpenseRepository.existsByVehicleIdAndCategoryId(dto.getVehicleId(), dto.getCategoryId())) {
                throw new PurchaseException("DUPLICATE_CATEGORY_EXPENSE",
                        "Expense with this category already exists for this vehicle");
            }
        }
        
        String currency = dto.getCurrency();
        if (currency == null || currency.trim().isEmpty()) {
            throw new PurchaseException("CURRENCY_REQUIRED", "Currency is required");
        }
        
        BigDecimal amount = dto.getAmount();
        BigDecimal exchangeRate = dto.getExchangeRate();
        BigDecimal convertedAmount = dto.getConvertedAmount();
        
        // Calculate exchange rate and converted amount if not provided
        if (!"EUR".equalsIgnoreCase(currency)) {
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                try {
                    exchangeRate = exchangeRateService.getExchangeRateToEur(currency);
                    if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new PurchaseException("EXCHANGE_RATE_NOT_FOUND", 
                                "Exchange rate not found for currency: " + currency);
                    }
                } catch (Exception e) {
                    log.error("Failed to get exchange rate for currency {}: {}", currency, e.getMessage());
                    throw new PurchaseException("FAILED_TO_GET_EXCHANGE_RATE", 
                            "Failed to get exchange rate for currency: " + currency);
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
                throw new PurchaseException("INVALID_EUR_CONVERSION", 
                        "For EUR currency, converted amount must equal amount");
            }
        }
        
        VehicleExpense expense = new VehicleExpense();
        expense.setVehicleId(dto.getVehicleId());
        expense.setFromAccountId(dto.getFromAccountId());
        expense.setCategoryId(dto.getCategoryId());
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setExchangeRate(exchangeRate);
        expense.setConvertedAmount(convertedAmount);
        expense.setDescription(dto.getDescription());
        
        return vehicleExpenseRepository.save(expense);
    }
    
    @Transactional(readOnly = true)
    public List<VehicleExpense> getExpensesByVehicleId(Long vehicleId) {
        return vehicleExpenseRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }
    
    @Transactional(readOnly = true)
    public java.util.Map<Long, List<VehicleExpense>> getExpensesByVehicleIds(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<VehicleExpense> expenses = vehicleExpenseRepository.findByVehicleIdInOrderByCreatedAtDesc(vehicleIds);
        return expenses.stream()
                .collect(java.util.stream.Collectors.groupingBy(VehicleExpense::getVehicleId));
    }
    
    @Transactional(readOnly = true)
    public VehicleExpense getExpenseById(Long expenseId) {
        return vehicleExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new PurchaseException("EXPENSE_NOT_FOUND",
                        String.format("Vehicle expense not found: id=%d", expenseId)));
    }
    
    @Transactional
    public VehicleExpense updateVehicleExpense(Long expenseId, VehicleExpenseUpdateDTO dto) {
        VehicleExpense expense = getExpenseById(expenseId);
        
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_AMOUNT", "Amount must be greater than zero");
        }
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(expense.getCategoryId())) {
            List<VehicleExpense> existingExpenses = vehicleExpenseRepository.findByVehicleIdOrderByCreatedAtDesc(expense.getVehicleId());
            boolean hasDuplicate = existingExpenses.stream()
                    .filter(e -> !e.getId().equals(expenseId))
                    .anyMatch(e -> dto.getCategoryId().equals(e.getCategoryId()));
            if (hasDuplicate) {
                throw new PurchaseException("DUPLICATE_CATEGORY_EXPENSE",
                        "Expense with this category already exists for this vehicle");
            }
        }
        
        String currency = dto.getCurrency();
        if (currency == null || currency.trim().isEmpty()) {
            throw new PurchaseException("CURRENCY_REQUIRED", "Currency is required");
        }
        
        BigDecimal amount = dto.getAmount();
        BigDecimal exchangeRate = dto.getExchangeRate();
        BigDecimal convertedAmount = dto.getConvertedAmount();
        
        if (!"EUR".equalsIgnoreCase(currency)) {
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                try {
                    exchangeRate = exchangeRateService.getExchangeRateToEur(currency);
                    if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new PurchaseException("EXCHANGE_RATE_NOT_FOUND", 
                                "Exchange rate not found for currency: " + currency);
                    }
                } catch (Exception e) {
                    log.error("Failed to get exchange rate for currency {}: {}", currency, e.getMessage());
                    throw new PurchaseException("FAILED_TO_GET_EXCHANGE_RATE", 
                            "Failed to get exchange rate for currency: " + currency);
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
                throw new PurchaseException("INVALID_EUR_CONVERSION", 
                        "For EUR currency, converted amount must equal amount");
            }
        }
        
        expense.setFromAccountId(dto.getFromAccountId());
        expense.setCategoryId(dto.getCategoryId());
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setExchangeRate(exchangeRate);
        expense.setConvertedAmount(convertedAmount);
        expense.setDescription(dto.getDescription());
        
        return vehicleExpenseRepository.save(expense);
    }
}

