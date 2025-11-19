package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.account.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "account-client", url = "http://localhost:8082/api/v1/accounts",
        configuration = FeignConfig.class)
public interface AccountClient {

    @GetMapping("/user/{userId}")
    List<AccountDTO> getAccountsByUserId(@PathVariable("userId") Long userId);

    @GetMapping
    List<AccountDTO> getAllAccounts();
}

