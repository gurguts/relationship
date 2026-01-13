package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
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
    
    private Long senderId;
    
    private Long receiverId;
    
    private Long destinationCountryId;
    
    private Long destinationPlaceId;
    
    private String product;
    
    private String productQuantity;
    
    private String declarationNumber;
    
    private Long terminalId;
    
    private String driverFullName;
    
    private Boolean eur1;
    
    private Boolean fito;
    
    private LocalDate customsDate;
    
    private LocalDate customsClearanceDate;
    
    private LocalDate unloadingDate;
    
    private LocalDate invoiceUaDate;
    
    private BigDecimal invoiceUaPricePerTon;
    
    private LocalDate invoiceEuDate;
    
    private BigDecimal invoiceEuPricePerTon;
    
    private BigDecimal reclamation;
    
    private Long carrierId;
}

