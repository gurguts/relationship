package org.example.warehouseservice.clients;

import org.example.warehouseservice.config.FeignConfig;
import org.example.warehouseservice.models.dto.PurchaseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "purchase-service", url = "http://localhost:8093/api/v1/purchase", configuration = FeignConfig.class)
public interface PurchaseApiClient {
    @GetMapping("/warehouse")
    List<PurchaseDTO> getPurchasesByFilters(@RequestParam("filters") Map<String, List<String>> filters);
}