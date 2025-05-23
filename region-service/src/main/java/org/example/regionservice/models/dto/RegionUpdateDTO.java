package org.example.regionservice.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegionUpdateDTO {
    @NotBlank(message = "{validation.region.notblank}")
    @Size(max = 255, message = "{validation.region.size}")
    private final String name;
}
