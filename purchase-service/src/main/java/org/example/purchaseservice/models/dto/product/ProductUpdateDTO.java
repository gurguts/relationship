package org.example.purchaseservice.models.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.purchaseservice.models.ProductUsage;

@Data
public class ProductUpdateDTO {
    @NotBlank(message = "{validation.product.name.notblank}")
    @Size(max = 255, message = "{validation.product.name.size}")
    private String name;

    @NotBlank(message = "{validation.product.usage.notblank}")
    @Size(max = 255, message = "{validation.product.usage.size}")
    private ProductUsage usage;
}
