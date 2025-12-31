package org.example.userservice.clients;

import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${purchase.service.url}/api/v1/product", configuration = FeignConfig.class)
public interface ProductApiClient {
    @GetMapping("/{productId}/name")
    String getProduct(@PathVariable("productId") Long productId);
}
