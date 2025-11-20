package org.example.purchaseservice.models.dto.balance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProductBalanceDTO {
    private Long id;
    private Long driverId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal averagePriceEur;
    private BigDecimal totalCostEur;
    private LocalDateTime updatedAt;
}

