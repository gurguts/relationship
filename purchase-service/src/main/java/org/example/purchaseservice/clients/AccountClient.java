package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.account.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "account-client", url = "${user.service.url}/api/v1/accounts",
        configuration = FeignConfig.class, contextId = "accountClient")
public interface AccountClient {

    @GetMapping("/user/{userId}")
    ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable("userId") @NonNull Long userId);

    @GetMapping
    ResponseEntity<List<AccountDTO>> getAllAccounts();
}
