package org.example.saleservice.clients;

import org.example.saleservice.config.FeignConfig;
import org.example.saleservice.models.dto.transaction.TransactionSaleCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "transaction-service", url = "http://localhost:8082/api/v1/transaction", configuration = FeignConfig.class)
public interface TransactionApiClient {

    @PatchMapping("/{transactionId}/amount")
    void updateTransactionAmount(@PathVariable("transactionId") Long transactionId, @RequestBody BigDecimal amount);

    @PostMapping("/sale")
    Long createTransactionSale(@RequestBody TransactionSaleCreateDTO transactionSaleCreateDTO);

    @DeleteMapping("/{transactionId}")
    void deleteTransaction(@PathVariable Long transactionId);
}
