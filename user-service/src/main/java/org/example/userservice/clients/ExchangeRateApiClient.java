package org.example.userservice.clients;

import lombok.NonNull;
import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "purchase-service", contextId = "exchangeRateApiClient",
        url = "${purchase.service.url}/api/v1/exchange-rates", configuration = FeignConfig.class)
public interface ExchangeRateApiClient {
    @GetMapping("/{currency}/rate")
    ResponseEntity<BigDecimal> getExchangeRateToEur(@PathVariable("currency") @NonNull String currency);
}

