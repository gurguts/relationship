package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleDestinationCountryCreateDTO {
    
    @NotBlank(message = "Destination country name is required")
    private String name;
}
