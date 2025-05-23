package org.example.warehouseservice.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WarehouseEntryDTO {
    private Long id;

    private Long userId;

    private Long productId;

    private BigDecimal quantity;

    private LocalDate entryDate;

    private BigDecimal purchasedQuantity;
}