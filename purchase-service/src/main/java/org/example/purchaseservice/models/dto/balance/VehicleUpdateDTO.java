package org.example.purchaseservice.models.dto.balance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VehicleUpdateDTO {
    private LocalDate shipmentDate;
    private String vehicleNumber;
    private String invoiceUa;
    private String invoiceEu;
    private String description;
    private String sender;
    private String receiver;
    private String destinationCountry;
    private String destinationPlace;
    private String product;
    private String productQuantity;
    private String declarationNumber;
    private String terminal;
    private String driverFullName;
    private Boolean isOurVehicle;
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

