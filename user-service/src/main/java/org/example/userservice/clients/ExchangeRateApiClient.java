package org.example.userservice.clients;

import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "purchase-service", contextId = "exchangeRateApiClient", url = "http://localhost:8093/api/v1/exchange-rates", configuration = FeignConfig.class)
public interface ExchangeRateApiClient {
    @GetMapping("/{currency}/rate")
    BigDecimal getExchangeRateToEur(@PathVariable("currency") String currency);
}

