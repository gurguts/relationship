package org.example.productservice.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.example.productservice.models.ProductUsage;

@Getter
@Setter
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotNull(message = "Usage must not be null")
    private ProductUsage usage;
}
