package org.example.clientservice.clients;

import org.example.clientservice.models.dto.product.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "product-service", url = "http://localhost:8093/api/v1/product")
public interface ProductClient {
    @GetMapping
    ResponseEntity<List<ProductDTO>> getAllProducts();
}
