package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;
import org.example.purchaseservice.models.dto.product.ProductCreateDTO;
import org.example.purchaseservice.models.dto.product.ProductDTO;
import org.example.purchaseservice.models.dto.product.ProductUpdateDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    public ProductDTO toDto(@NonNull Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setUsage(product.getUsage());
        return dto;
    }

    public List<ProductDTO> toDtoList(@NonNull List<Product> products) {
        return products.stream()
                .map(this::toDto)
                .toList();
    }

    public Product productCreateDTOToProduct(@NonNull ProductCreateDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setUsage(ProductUsage.valueOf(dto.getUsage()));
        return product;
    }

    public Product productUpdateDTOToProduct(@NonNull ProductUpdateDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setUsage(dto.getUsage());
        return product;
    }
}