package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WarehouseWithdrawalUpdateDTO {
    private Long productId;
    private Long withdrawalReasonId;
    private Double quantity;
    private String description;
    private LocalDate withdrawalDate;
}
