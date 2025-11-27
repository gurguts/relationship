package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseReceiptCreateDTO {

    private Long userId;

    private Long productId;

    private Long warehouseId;

    private BigDecimal quantity;

    private Long typeId;

    private BigDecimal purchasedQuantity;
}

