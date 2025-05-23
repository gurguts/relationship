package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WithdrawalRequestDTO {
    private Long productId;
    private String reasonType;
    private Double quantity;
    private String description;
    private LocalDate withdrawalDate;
}