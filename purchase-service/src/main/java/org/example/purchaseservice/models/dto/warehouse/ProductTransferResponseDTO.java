package org.example.purchaseservice.models.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTransferResponseDTO {
    private Long id;
    private Long warehouseId;
    private Long fromProductId;
    private Long toProductId;
    private BigDecimal quantity;
    private BigDecimal unitPriceUah;
    private BigDecimal totalCostUah;
    private LocalDate transferDate;
    private Long userId;
    private Long reasonId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

