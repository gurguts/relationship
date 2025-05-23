package org.example.sourceservice.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SourceUpdateDTO {
    @NotBlank(message = "{validation.source.notblank}")
    @Size(max = 255, message = "{validation.source.size}")
    private String name;
}
