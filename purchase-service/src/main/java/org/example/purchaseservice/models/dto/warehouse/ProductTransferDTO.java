package org.example.purchaseservice.models.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTransferDTO {
    private Long warehouseId;
    private Long fromProductId;
    private Long toProductId;
    private BigDecimal quantity;
    private Long withdrawalReasonId;
    private String description;
}

