package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.clients.AccountClient;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.account.AccountDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseAccountService {
    
    private static final String DEFAULT_TRANSACTION_CURRENCY = "UAH";
    private static final String UAH_CURRENCY = "UAH";
    
    private final AccountClient accountClient;
    
    public List<AccountDTO> getUserAccounts(@NonNull Long userId) {
        try {
            List<AccountDTO> userAccounts = accountClient.getAccountsByUserId(userId).getBody();
            return userAccounts != null ? userAccounts : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    public void validateUserHasAccounts(@NonNull Long userId, @NonNull List<AccountDTO> userAccounts) {
        if (userAccounts.isEmpty()) {
            throw new PurchaseException("NO_ACCOUNTS", 
                    String.format("У користувача з ID %d немає рахунків. Створіть рахунок перед створенням закупки.", userId));
        }
    }
    
    public AccountDTO findAccountForTransaction(@NonNull Long userId, @NonNull List<AccountDTO> userAccounts, 
                                               @NonNull String transactionCurrency) {
        AccountDTO account = findAccountWithCurrency(userAccounts, transactionCurrency);
        
        if (account == null) {
            throw new PurchaseException("NO_ACCOUNT_WITH_CURRENCY", 
                    String.format("У користувача з ID %d немає рахунку з валютою %s. Створіть рахунок з цією валютою.", 
                            userId, transactionCurrency));
        }
        
        return account;
    }
    
    public AccountDTO findAccountWithCurrency(@NonNull List<AccountDTO> accounts, String currency) {
        if (accounts.isEmpty()) {
            return null;
        }
        
        if (currency == null || currency.isEmpty() || UAH_CURRENCY.equals(currency)) {
            return accounts.stream()
                    .filter(acc -> acc != null && acc.getCurrencies() != null && 
                            (acc.getCurrencies().contains(UAH_CURRENCY) || acc.getCurrencies().isEmpty()))
                    .findFirst()
                    .orElse(null);
        }

        return accounts.stream()
                .filter(acc -> acc != null && acc.getCurrencies() != null && acc.getCurrencies().contains(currency))
                .findFirst()
                .orElse(null);
    }
    
    public String getTransactionCurrencyOrDefault(String currency) {
        return currency != null ? currency : DEFAULT_TRANSACTION_CURRENCY;
    }
}
