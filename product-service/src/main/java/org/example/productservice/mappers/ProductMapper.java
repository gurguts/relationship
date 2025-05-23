package org.example.productservice.mappers;

import org.example.productservice.models.Product;
import org.example.productservice.models.dto.ProductCreateDTO;
import org.example.productservice.models.dto.ProductDTO;
import org.example.productservice.models.dto.ProductUpdateDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public ProductDTO toDto(Product product) {
        if (product == null) {
            return null;
        }
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setUsage(product.getUsage());
        return dto;
    }

    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setUsage(dto.getUsage());
        return product;
    }

    public List<ProductDTO> toDtoList(List<Product> products) {
        if (products == null) {
            return null;
        }
        return products.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Product productCreateDTOToProduct(ProductCreateDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setUsage(dto.getUsage());
        return product;
    }

    public Product productUpdateDTOToProduct(ProductUpdateDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setUsage(dto.getUsage());
        return product;
    }
}