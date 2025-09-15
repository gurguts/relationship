package org.example.purchaseservice.models.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.purchaseservice.models.ProductUsage;

@Data
public class ProductUpdateDTO {
    @NotBlank(message = "{validation.product.name.notblank}")
    @Size(max = 255, message = "{validation.product.name.size}")
    private String name;

    @NotNull(message = "{validation.product.usage.notnull}")
    private ProductUsage usage;
}
