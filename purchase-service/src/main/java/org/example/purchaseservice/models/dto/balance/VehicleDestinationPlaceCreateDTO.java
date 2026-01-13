package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleDestinationPlaceCreateDTO {
    
    @NotBlank(message = "Destination place name is required")
    private String name;
}
