package org.example.warehouseservice.models.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseDTO {
    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal quantity;
    private LocalDateTime createdAt;
}