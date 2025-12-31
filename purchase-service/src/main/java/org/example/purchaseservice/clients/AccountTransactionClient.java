package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.purchaseservice.models.dto.transaction.TransactionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "account-transaction-client", url = "${user.service.url}/api/v1/transaction",
        configuration = FeignConfig.class)
public interface AccountTransactionClient {

    @PostMapping
    TransactionDTO createTransaction(@RequestBody TransactionCreateRequestDTO request);
}

