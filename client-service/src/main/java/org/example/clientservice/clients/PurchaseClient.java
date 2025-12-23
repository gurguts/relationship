package org.example.clientservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "purchase-service", url = "http://localhost:8093/api/v1/purchase")
public interface PurchaseClient {
    @GetMapping("/client/{clientId}")
    ResponseEntity<List<Map<String, Object>>> getPurchasesByClientId(@PathVariable("clientId") Long clientId);
}

