package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseUpdateDTO;
import org.example.purchaseservice.repositories.VehicleExpenseRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.services.exchange.IExchangeRateService;
import org.example.purchaseservice.services.impl.IVehicleExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpenseService implements IVehicleExpenseService {
    
    private static final int EXCHANGE_RATE_SCALE = 6;
    private static final String EUR_CURRENCY = "EUR";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleRepository vehicleRepository;
    private final IExchangeRateService exchangeRateService;
    
    @Override
    @Transactional
    public VehicleExpense createVehicleExpense(@NonNull VehicleExpenseCreateDTO dto) {
        log.info("Creating vehicle expense: vehicleId={}, categoryId={}", dto.getVehicleId(), dto.getCategoryId());

        validateVehicleExists(dto.getVehicleId());
        validateAmount(dto.getAmount());
        
        if (dto.getCategoryId() != null) {
            if (vehicleExpenseRepository.existsByVehicleIdAndCategoryId(dto.getVehicleId(), dto.getCategoryId())) {
                throw new PurchaseException("DUPLICATE_CATEGORY_EXPENSE",
                        "Expense with this category already exists for this vehicle");
            }
        }
        
        String currency = normalizeCurrency(dto.getCurrency());
        
        ConversionResult conversionResult = convertCurrency(
                dto.getAmount(),
                currency,
                dto.getExchangeRate(),
                dto.getConvertedAmount()
        );
        
        VehicleExpense expense = new VehicleExpense();
        expense.setVehicleId(dto.getVehicleId());
        expense.setFromAccountId(dto.getFromAccountId());
        expense.setCategoryId(dto.getCategoryId());
        expense.setAmount(dto.getAmount());
        expense.setCurrency(currency);
        expense.setExchangeRate(conversionResult.exchangeRate());
        expense.setConvertedAmount(conversionResult.convertedAmount());
        expense.setDescription(dto.getDescription());
        
        VehicleExpense saved = vehicleExpenseRepository.save(expense);
        log.info("Vehicle expense created: id={}, vehicleId={}, amount={}, currency={}", 
                saved.getId(), saved.getVehicleId(), saved.getAmount(), saved.getCurrency());
        
        return saved;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VehicleExpense> getExpensesByVehicleId(@NonNull Long vehicleId) {
        return vehicleExpenseRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<VehicleExpense>> getExpensesByVehicleIds(@NonNull List<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (vehicleIds.stream().anyMatch(Objects::isNull)) {
            throw new PurchaseException("INVALID_VEHICLE_ID", "Vehicle ID list cannot contain null values");
        }
        List<VehicleExpense> expenses = vehicleExpenseRepository.findByVehicleIdInOrderByCreatedAtDesc(vehicleIds);
        return expenses.stream()
                .collect(Collectors.groupingBy(VehicleExpense::getVehicleId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public VehicleExpense getExpenseById(@NonNull Long expenseId) {
        return vehicleExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new PurchaseException("EXPENSE_NOT_FOUND",
                        String.format("Vehicle expense not found: id=%d", expenseId)));
    }
    
    @Override
    @Transactional
    public VehicleExpense updateVehicleExpense(@NonNull Long expenseId, @NonNull VehicleExpenseUpdateDTO dto) {
        log.info("Updating vehicle expense: id={}", expenseId);
        
        VehicleExpense expense = getExpenseById(expenseId);
        
        validateAmount(dto.getAmount());
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(expense.getCategoryId())) {
            if (vehicleExpenseRepository.existsByVehicleIdAndCategoryIdAndIdNot(
                    expense.getVehicleId(), dto.getCategoryId(), expenseId)) {
                throw new PurchaseException("DUPLICATE_CATEGORY_EXPENSE",
                        "Expense with this category already exists for this vehicle");
            }
        }
        
        String currency = normalizeCurrency(dto.getCurrency());
        
        ConversionResult conversionResult = convertCurrency(
                dto.getAmount(),
                currency,
                dto.getExchangeRate(),
                dto.getConvertedAmount()
        );
        
        expense.setFromAccountId(dto.getFromAccountId());
        expense.setCategoryId(dto.getCategoryId());
        expense.setAmount(dto.getAmount());
        expense.setCurrency(currency);
        expense.setExchangeRate(conversionResult.exchangeRate());
        expense.setConvertedAmount(conversionResult.convertedAmount());
        expense.setDescription(dto.getDescription());
        
        VehicleExpense saved = vehicleExpenseRepository.save(expense);
        log.info("Vehicle expense updated: id={}, vehicleId={}, amount={}, currency={}", 
                saved.getId(), saved.getVehicleId(), saved.getAmount(), saved.getCurrency());
        
        return saved;
    }
    
    private record ConversionResult(BigDecimal exchangeRate, BigDecimal convertedAmount) {}

    private void validateVehicleExists(Long vehicleId) {
        if (vehicleId == null) {
            throw new PurchaseException("VEHICLE_ID_REQUIRED", "Vehicle ID is required");
        }
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new PurchaseException("VEHICLE_NOT_FOUND",
                    String.format("Vehicle not found: id=%d", vehicleId));
        }
    }
    
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new PurchaseException("INVALID_AMOUNT", "Amount must be greater than zero");
        }
    }
    
    private String normalizeCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new PurchaseException("CURRENCY_REQUIRED", "Currency is required");
        }
        return currency.trim().toUpperCase();
    }
    
    private ConversionResult convertCurrency(@NonNull BigDecimal amount, @NonNull String currency, 
                                            BigDecimal exchangeRate, BigDecimal convertedAmount) {
        validateAmount(amount);
        
        if (!EUR_CURRENCY.equals(currency)) {
            if (exchangeRate == null || exchangeRate.compareTo(ZERO) <= 0) {
                try {
                    exchangeRate = exchangeRateService.getExchangeRateToEur(currency);
                    if (exchangeRate == null || exchangeRate.compareTo(ZERO) <= 0) {
                        throw new PurchaseException("EXCHANGE_RATE_NOT_FOUND", 
                                "Exchange rate not found for currency: " + currency);
                    }
                } catch (PurchaseException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to get exchange rate for currency {}: {}", currency, e.getMessage(), e);
                    throw new PurchaseException("FAILED_TO_GET_EXCHANGE_RATE", 
                            "Failed to get exchange rate for currency: " + currency);
                }
            }
            
            if (convertedAmount == null || convertedAmount.compareTo(ZERO) <= 0) {
                if (exchangeRate.compareTo(ZERO) == 0) {
                    throw new PurchaseException("INVALID_EXCHANGE_RATE", 
                            "Exchange rate cannot be zero for division");
                }
                convertedAmount = amount.divide(exchangeRate, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            } else {
                if (convertedAmount.compareTo(ZERO) == 0) {
                    throw new PurchaseException("INVALID_CONVERTED_AMOUNT", 
                            "Converted amount cannot be zero for exchange rate calculation");
                }
                exchangeRate = amount.divide(convertedAmount, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            }
        } else {
            exchangeRate = BigDecimal.ONE;
            if (convertedAmount == null || convertedAmount.compareTo(ZERO) <= 0) {
                convertedAmount = amount;
            } else if (convertedAmount.compareTo(amount) != 0) {
                throw new PurchaseException("INVALID_EUR_CONVERSION", 
                        "For EUR currency, converted amount must equal amount");
            }
        }
        
        return new ConversionResult(exchangeRate, convertedAmount);
    }
}

