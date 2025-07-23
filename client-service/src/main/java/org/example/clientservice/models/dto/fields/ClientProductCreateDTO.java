package org.example.clientservice.models.dto.fields;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientProductCreateDTO (
        @NotBlank(message = "{validation.clientProduct.notblank}") @Size(max = 255, message = "{validation.clientProduct.size}")
        String name) {
}