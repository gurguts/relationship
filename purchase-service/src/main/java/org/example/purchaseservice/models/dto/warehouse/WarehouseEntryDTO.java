package org.example.purchaseservice.models.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WarehouseEntryDTO {
    private Long id;

    private Long userId;

    private Long productId;

    private Long warehouseId;

    private BigDecimal quantity;

    private LocalDate entryDate;

    private WithdrawalReason type;

    private BigDecimal purchasedQuantity;
}