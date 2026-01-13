package org.example.purchaseservice.models.dto.balance;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VehicleDestinationPlaceDTO {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
