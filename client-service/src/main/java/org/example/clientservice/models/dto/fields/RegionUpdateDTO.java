package org.example.clientservice.models.dto.fields;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionUpdateDTO(
        @NotBlank(message = "{validation.region.notblank}") @Size(max = 255, message = "{validation.region.size}")
        String name) {
}
