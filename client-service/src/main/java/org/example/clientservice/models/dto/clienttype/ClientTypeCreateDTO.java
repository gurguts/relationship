package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

@Data
public class ClientTypeCreateDTO {
    @NonNull
    @NotBlank
    @Size(max = 255)
    private String name;
    
    @NonNull
    @NotBlank
    @Size(max = 255)
    private String nameFieldLabel;
}

