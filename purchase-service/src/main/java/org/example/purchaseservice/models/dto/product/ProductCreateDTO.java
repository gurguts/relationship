package org.example.purchaseservice.models.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductCreateDTO {
    @NotBlank(message = "{validation.product.name.notblank}")
    @Size(max = 255, message = "{validation.product.name.size}")
    private String name;

    @NotBlank(message = "{validation.product.usage.notblank}")
    @Size(max = 50, message = "{validation.product.usage.size}")
    private String usage;
}
