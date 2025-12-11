package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VehicleCreateDTO {
    
    @NotNull(message = "Shipment date is required")
    private LocalDate shipmentDate;
    
    private String vehicleNumber;
    
    private String invoiceUa;
    
    private String invoiceEu;
    
    private String description;
    
    private Boolean isOurVehicle;
}

