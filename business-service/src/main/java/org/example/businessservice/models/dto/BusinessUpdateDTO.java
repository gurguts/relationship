package org.example.businessservice.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessUpdateDTO {

    @NotBlank(message = "{validation.business.notblank}")
    @Size(max = 255, message = "{validation.business.size}")
    private String name;
}
