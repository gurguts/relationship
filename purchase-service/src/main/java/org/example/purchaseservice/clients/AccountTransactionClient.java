package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-transaction-client", url = "${user.service.url}/api/v1/transaction",
        configuration = FeignConfig.class, contextId = "accountTransactionClient")
public interface AccountTransactionClient {

    @PostMapping
    ResponseEntity<TransactionDTO> createTransaction(@RequestBody @NonNull TransactionCreateRequestDTO request);
}
