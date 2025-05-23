package org.example.containerservice.models.dto.container;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContainerCreateDTO {
    @NotBlank(message = "{validation.container.notblank}")
    @Size(max = 255, message = "{validation.container.size}")
    private String name;
}
