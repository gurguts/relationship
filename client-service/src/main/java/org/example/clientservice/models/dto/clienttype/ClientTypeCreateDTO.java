package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientTypeCreateDTO {
    @NotBlank
    @Size(max = 255)
    private String name;
    
    @NotBlank
    @Size(max = 255)
    private String nameFieldLabel;
}

