package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;

@Data
public class WithdrawalCreateDTO {
    private Long productId;
    private Long warehouseId;
    private Long withdrawalReasonId;
    private Double quantity;
    private String description;
}