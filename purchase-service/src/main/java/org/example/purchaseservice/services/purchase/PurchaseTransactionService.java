package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.AccountTransactionClient;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.account.AccountDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionDTO;
import org.example.purchaseservice.models.transaction.TransactionType;
import org.springframework.stereotype.Service;
import feign.FeignException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTransactionService {
    
    private final TransactionApiClient transactionApiClient;
    private final AccountTransactionClient accountTransactionClient;
    private final PurchaseAccountService accountService;
    
    public Long createAccountTransactionForPurchase(@NonNull Long userId, @NonNull Purchase purchase) {
        List<AccountDTO> userAccounts = accountService.getUserAccounts(userId);
        accountService.validateUserHasAccounts(userId, userAccounts);
        
        String transactionCurrency = accountService.getTransactionCurrencyOrDefault(purchase.getCurrency());
        AccountDTO account = accountService.findAccountForTransaction(userId, userAccounts, transactionCurrency);
        
        TransactionCreateRequestDTO transactionRequest = buildTransactionRequest(account, purchase, transactionCurrency);
        return executeTransactionCreation(transactionRequest);
    }
    
    public void updateTransactionAmount(@NonNull Long transactionId, @NonNull java.math.BigDecimal newAmount) {
        try {
            transactionApiClient.updateTransactionAmount(transactionId, newAmount);
        } catch (FeignException e) {
            log.error("Feign error updating transaction amount: transactionId={}, status={}, error={}", 
                    transactionId, e.status(), e.getMessage(), e);
            throw new PurchaseException("TRANSACTION_UPDATE_FAILED", 
                    String.format("Failed to update transaction amount: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error updating transaction amount: transactionId={}", transactionId, e);
            throw new PurchaseException("TRANSACTION_UPDATE_FAILED", 
                    String.format("Failed to update transaction amount: %s", e.getMessage()), e);
        }
    }
    
    public void deletePurchaseTransaction(@NonNull Purchase purchase) {
        if (purchase.getTransaction() != null) {
            try {
                transactionApiClient.deleteTransaction(purchase.getTransaction());
            } catch (RuntimeException _) {
            }
        }
    }
    
    private TransactionCreateRequestDTO buildTransactionRequest(@NonNull AccountDTO account, 
                                                                 @NonNull Purchase purchase, 
                                                                 @NonNull String transactionCurrency) {
        TransactionCreateRequestDTO transactionRequest = new TransactionCreateRequestDTO();
        transactionRequest.setType(TransactionType.CLIENT_PAYMENT);
        transactionRequest.setFromAccountId(account.getId());
        transactionRequest.setAmount(purchase.getTotalPrice());
        transactionRequest.setCurrency(transactionCurrency);
        transactionRequest.setClientId(purchase.getClient());
        transactionRequest.setDescription(purchase.getComment());
        return transactionRequest;
    }
    
    private Long executeTransactionCreation(@NonNull TransactionCreateRequestDTO transactionRequest) {
        try {
            TransactionDTO transactionDTO = accountTransactionClient.createTransaction(transactionRequest).getBody();
            if (transactionDTO == null || transactionDTO.getId() == null) {
                throw new PurchaseException("TRANSACTION_CREATION_FAILED", 
                        "Failed to create transaction: response body or transaction ID is null");
            }
            return transactionDTO.getId();
        } catch (FeignException e) {
            log.error("Feign error creating transaction: accountId={}, status={}, error={}", 
                    transactionRequest.getFromAccountId(), e.status(), e.getMessage());
            throw new PurchaseException("TRANSACTION_CREATION_FAILED", 
                    String.format("Failed to create transaction: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error creating transaction: accountId={}", 
                    transactionRequest.getFromAccountId(), e);
            throw new PurchaseException("TRANSACTION_CREATION_FAILED", 
                    String.format("Failed to create transaction: %s", e.getMessage()), e);
        }
    }
}
