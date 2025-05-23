package org.example.warehouseservice.clients;

import org.example.warehouseservice.config.FeignConfig;
import org.example.warehouseservice.models.dto.ProductDTO;
import org.example.warehouseservice.models.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", url = "http://localhost:8092/api/v1/product", configuration = FeignConfig.class)
public interface ProductApiClient {
    @GetMapping
    List<ProductDTO> getProducts();
}
