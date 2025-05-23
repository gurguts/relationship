package org.example.containerservice.clients;

import org.example.containerservice.models.dto.fields.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "product-service", url = "http://localhost:8093/api/v1/product")
public interface ProductClient {
    @GetMapping
    List<ProductDTO> getAllProduct();
}
