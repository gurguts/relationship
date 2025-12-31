package org.example.clientservice.models.dto.fields;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

@Data
public class SourceUpdateDTO {
    @NonNull
    @NotBlank(message = "{validation.source.notblank}")
    @Size(max = 255, message = "{validation.source.size}")
    private String name;
    
    private Long userId;
}
