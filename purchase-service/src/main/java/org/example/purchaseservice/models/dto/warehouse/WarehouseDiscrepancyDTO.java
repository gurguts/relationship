package org.example.purchaseservice.models.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDiscrepancyDTO {
    private Long id;
    private Long warehouseReceiptId;
    private Long driverId;
    private Long productId;
    private Long warehouseId;
    private LocalDate receiptDate;
    private BigDecimal purchasedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal discrepancyQuantity;
    private BigDecimal unitPriceEur;
    private BigDecimal discrepancyValueEur;
    private String type;
    private String comment;
    private Long createdByUserId;
    private LocalDateTime createdAt;
}

