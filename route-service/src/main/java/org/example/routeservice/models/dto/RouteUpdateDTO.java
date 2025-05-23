package org.example.routeservice.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RouteUpdateDTO {
    @NotBlank(message = "{validation.route.notblank}")
    @Size(max = 255, message = "{validation.route.size}")
    private final String name;
}
