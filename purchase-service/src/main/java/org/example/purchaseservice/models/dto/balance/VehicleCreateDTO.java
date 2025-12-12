package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VehicleCreateDTO {
    
    private LocalDate shipmentDate;
    
    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;
    
    private String invoiceUa;
    
    private String invoiceEu;
    
    private String description;
    
    private Boolean isOurVehicle;
    
    private String sender;
    
    private String receiver;
    
    private String destinationCountry;
    
    private String destinationPlace;
    
    private String product;
    
    private String productQuantity;
    
    private String declarationNumber;
    
    private String terminal;
    
    private String driverFullName;
    
    private Boolean eur1;
    
    private Boolean fito;
    
    private LocalDate customsDate;
    
    private LocalDate customsClearanceDate;
    
    private LocalDate unloadingDate;
    
    private Long carrierId;
}

