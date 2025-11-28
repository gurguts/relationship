package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientTypeUpdateDTO {
    @Size(max = 255)
    private String name;
    
    @Size(max = 255)
    private String nameFieldLabel;
    
    private Boolean isActive;
}

