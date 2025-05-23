package org.example.clientservice.models.dto.product;

import lombok.Data;

@Data
public class ProductDTO {
    private Long id;

    private String name;

    private ProductUsage usage;
}
