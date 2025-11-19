package org.example.purchaseservice.models.dto.balance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WarehouseProductBalanceDTO {
    private Long id;
    private Long warehouseId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal averagePriceUah;
    private BigDecimal totalCostUah;
    private LocalDateTime updatedAt;
}

