package org.example.transactionservice.clients;

import org.example.transactionservice.config.FeignConfig;
import org.example.transactionservice.models.dto.BalanceUpdateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "balance-service", url = "http://localhost:8085/api/v1/balance", configuration = FeignConfig.class)
public interface BalanceApiClient {


/*
    @GetMapping("/user/{userId}")
    ResponseEntity<BigDecimal> getUserBalance(@PathVariable("userId") Long balanceId);
*/

    @PatchMapping("/user/{userId}")
    void updateUserBalance(@PathVariable("userId") Long userId, @RequestBody BalanceUpdateDTO balanceUpdateDTO);
}
