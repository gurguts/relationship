package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleReceiverCreateDTO {
    
    @NotBlank(message = "Name is required")
    private String name;
}

