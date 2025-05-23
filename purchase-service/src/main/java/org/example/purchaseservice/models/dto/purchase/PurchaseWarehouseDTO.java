package org.example.purchaseservice.models.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseWarehouseDTO {
    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal quantity;
    private LocalDateTime createdAt;
}
