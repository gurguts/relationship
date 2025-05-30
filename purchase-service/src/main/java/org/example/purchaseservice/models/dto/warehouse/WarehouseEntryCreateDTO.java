package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WarehouseEntryCreateDTO {

    private Long userId;

    private Long productId;

    private Long warehouseId;

    private BigDecimal quantity;

    private LocalDate entryDate;

    private BigDecimal purchasedQuantity;
}
