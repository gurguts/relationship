package org.example.clientservice.models.dto.fields;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StatusClientUpdateDTO {
    @NotBlank(message = "{validation.statusClient.notblank}")
    @Size(max = 255, message = "{validation.statusClient.size}")
    private String name;
}
