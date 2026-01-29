package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseUpdateDTO;
import org.example.purchaseservice.repositories.VehicleExpenseRepository;
import org.example.purchaseservice.services.impl.IVehicleExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpenseService implements IVehicleExpenseService {
    
    private final VehicleExpenseRepository vehicleExpenseRepository;
    private final VehicleExpenseValidator validator;
    private final VehicleExpenseCurrencyConverter currencyConverter;
    
    @Override
    @Transactional
    public VehicleExpense createVehicleExpense(@NonNull VehicleExpenseCreateDTO dto) {
        log.info("Creating vehicle expense: vehicleId={}, categoryId={}", dto.getVehicleId(), dto.getCategoryId());

        validator.validateVehicleExists(dto.getVehicleId());
        validator.validateAmount(dto.getAmount());
        validator.validateNoDuplicateCategory(dto.getVehicleId(), dto.getCategoryId());
        
        String currency = validator.normalizeCurrency(dto.getCurrency());
        
        VehicleExpenseCurrencyConverter.ConversionResult conversionResult = currencyConverter.convertCurrency(
                dto.getAmount(),
                currency,
                dto.getExchangeRate(),
                dto.getConvertedAmount()
        );
        
        VehicleExpense expense = new VehicleExpense();
        expense.setVehicleId(dto.getVehicleId());
        mapDtoToExpense(expense, dto.getFromAccountId(), dto.getCategoryId(), dto.getAmount(), 
                currency, conversionResult, dto.getDescription());
        
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
        validator.validateVehicleIds(vehicleIds);
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
        
        validator.validateAmount(dto.getAmount());
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(expense.getCategoryId())) {
            validator.validateNoDuplicateCategoryOnUpdate(expense.getVehicleId(), dto.getCategoryId(), expenseId);
        }
        
        String currency = validator.normalizeCurrency(dto.getCurrency());
        
        VehicleExpenseCurrencyConverter.ConversionResult conversionResult = currencyConverter.convertCurrency(
                dto.getAmount(),
                currency,
                dto.getExchangeRate(),
                dto.getConvertedAmount()
        );
        
        mapDtoToExpense(expense, dto.getFromAccountId(), dto.getCategoryId(), dto.getAmount(), 
                currency, conversionResult, dto.getDescription());
        
        VehicleExpense saved = vehicleExpenseRepository.save(expense);
        log.info("Vehicle expense updated: id={}, vehicleId={}, amount={}, currency={}", 
                saved.getId(), saved.getVehicleId(), saved.getAmount(), saved.getCurrency());
        
        return saved;
    }
    
    @Override
    @Transactional
    public void deleteVehicleExpense(@NonNull Long expenseId) {
        log.info("Deleting vehicle expense: id={}", expenseId);
        
        VehicleExpense expense = getExpenseById(expenseId);
        vehicleExpenseRepository.delete(expense);
        
        log.info("Vehicle expense deleted: id={}", expenseId);
    }

    private void mapDtoToExpense(@NonNull VehicleExpense expense,
                                Long fromAccountId,
                                Long categoryId,
                                @NonNull java.math.BigDecimal amount,
                                @NonNull String currency,
                                @NonNull VehicleExpenseCurrencyConverter.ConversionResult conversionResult) {
        expense.setFromAccountId(fromAccountId);
        expense.setCategoryId(categoryId);
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setExchangeRate(conversionResult.exchangeRate());
        expense.setConvertedAmount(conversionResult.convertedAmount());
    }

    private void mapDtoToExpense(@NonNull VehicleExpense expense,
                                Long fromAccountId,
                                Long categoryId,
                                @NonNull java.math.BigDecimal amount,
                                @NonNull String currency,
                                @NonNull VehicleExpenseCurrencyConverter.ConversionResult conversionResult,
                                String description) {
        mapDtoToExpense(expense, fromAccountId, categoryId, amount, currency, conversionResult);
        expense.setDescription(description);
    }
    
}

