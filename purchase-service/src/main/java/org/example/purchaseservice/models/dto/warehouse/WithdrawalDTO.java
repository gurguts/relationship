package org.example.purchaseservice.models.dto.warehouse;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalDTO {
    private Long id;
    private Long productId;
    private Long userId;
    private String reasonType;
    private Double quantity;
    private String description;
    private LocalDate withdrawalDate;
    private LocalDateTime createdAt;
}
