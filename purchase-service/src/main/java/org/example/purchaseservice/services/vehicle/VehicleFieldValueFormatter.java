package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VehicleFieldValueFormatter {
    
    public static final String UNKNOWN_PRODUCT = "Невідомий товар";
    public static final String PRODUCT_PREFIX = "Товар #";
    public static final String ACCOUNT_PREFIX = "Рахунок #";
    public static final String EUR_SUFFIX = " EUR";
    public static final String KG_SUFFIX = " кг";
    
    public String formatNumber(BigDecimal value) {
        if (value == null) return "";
        return String.format("%.2f", value);
    }
    
    public String getProductName(Long productId, @NonNull Map<Long, Product> productMap) {
        if (productId == null) {
            return UNKNOWN_PRODUCT;
        }
        Product product = productMap.get(productId);
        return product != null ? product.getName() : PRODUCT_PREFIX + productId;
    }
    
    public String getAccountName(Long accountId, @NonNull Map<Long, String> accountNameMap) {
        if (accountId == null) {
            return "";
        }
        return accountNameMap.getOrDefault(accountId, ACCOUNT_PREFIX + accountId);
    }
    
    public String formatExpenseDetails(@NonNull List<org.example.purchaseservice.models.balance.VehicleExpense> categoryExpenses,
                                       @NonNull Map<Long, String> accountNameMap) {
        if (isEmpty(categoryExpenses)) {
            return "";
        }
        
        StringBuilder expenseDetails = new StringBuilder();
        for (org.example.purchaseservice.models.balance.VehicleExpense expense : categoryExpenses) {
            if (!expenseDetails.isEmpty()) {
                expenseDetails.append("\n");
            }
            
            String accountName = getAccountName(expense.getFromAccountId(), accountNameMap);
            String currency = safeString(expense.getCurrency());
            String exchangeRate = safeFormatNumber(expense.getExchangeRate());
            String description = safeString(expense.getDescription());
            String amountEur = safeFormatNumber(expense.getConvertedAmount()) + EUR_SUFFIX;
            String originalAmount = safeFormatNumber(expense.getAmount());
            
            expenseDetails.append(String.format("%s %s (курс: %s) = %s", 
                    originalAmount, currency, exchangeRate, amountEur));
            
            if (isNotEmpty(accountName)) {
                expenseDetails.append(", Рахунок: ").append(accountName);
            }
            if (isNotEmpty(description)) {
                expenseDetails.append(", Опис: ").append(description);
            }
        }
        return expenseDetails.toString();
    }
    
    public String formatBoolean(Boolean value) {
        return value != null && value ? "Так" : "Ні";
    }
    
    private String safeString(String value) {
        return value != null ? value : "";
    }
    
    private String safeFormatNumber(BigDecimal value) {
        return value != null ? formatNumber(value) : "";
    }
    
    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
    
    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
