package org.example.statusclientservice.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusClientCreateDTO {
    @NotBlank(message = "{validation.statusClient.notblank}")
    @Size(max = 255, message = "{validation.statusClient.size}")
    private String name;
}
