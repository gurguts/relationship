package org.example.clientservice.clients;

import org.example.clientservice.config.FeignConfig;
import org.example.clientservice.models.dto.product.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "purchase-service", url = "${purchase.service.url}/api/v1/product",
        configuration = FeignConfig.class, contextId = "productClient")
public interface ProductClient {
    @GetMapping
    ResponseEntity<List<ProductDTO>> getAllProducts();
}
