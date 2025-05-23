package org.example.clientservice.models.dto.fields;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RouteUpdateDTO(
        @NotBlank(message = "{validation.route.notblank}") @Size(max = 255, message = "{validation.route.size}")
        String name) {
}
