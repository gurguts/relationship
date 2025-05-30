package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WarehouseWithdrawalDTO {
    private Long id;

    private Long productId;

    private Long userId;

    private Long warehouseId;

    private WithdrawalReason reasonType;

    private BigDecimal quantity;

    private String description;

    private LocalDate withdrawalDate;

    private LocalDateTime createdAt;
}
