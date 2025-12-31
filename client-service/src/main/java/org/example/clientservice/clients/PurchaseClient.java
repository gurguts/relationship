package org.example.clientservice.clients;

import lombok.NonNull;
import org.example.clientservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "purchase-service", url = "${purchase.service.url}/api/v1/purchase",
        configuration = FeignConfig.class, contextId = "purchaseClient")
public interface PurchaseClient {
    @GetMapping("/client/{clientId}")
    ResponseEntity<List<Map<String, Object>>> getPurchasesByClientId(@NonNull @PathVariable("clientId") Long clientId);
}

