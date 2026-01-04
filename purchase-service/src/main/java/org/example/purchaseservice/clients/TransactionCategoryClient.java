package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "transaction-category-client", url = "${user.service.url}/api/v1/transaction-categories",
        configuration = FeignConfig.class, contextId = "transactionCategoryClient")
public interface TransactionCategoryClient {

    @GetMapping("/{id}")
    ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable("id") @NonNull Long id);
}
