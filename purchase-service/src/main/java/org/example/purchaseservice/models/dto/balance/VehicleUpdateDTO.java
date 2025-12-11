package org.example.purchaseservice.models.dto.balance;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VehicleUpdateDTO {
    private LocalDate shipmentDate;
    private String vehicleNumber;
    private String invoiceUa;
    private String invoiceEu;
    private String description;
}

