package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleTerminalCreateDTO {
    
    @NotBlank(message = "Terminal name is required")
    private String name;
}
