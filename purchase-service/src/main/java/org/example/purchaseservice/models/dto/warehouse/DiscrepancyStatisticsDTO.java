package org.example.purchaseservice.models.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyStatisticsDTO {
    private BigDecimal totalLossesValue;
    private BigDecimal totalGainsValue;
    private BigDecimal netValue;
    private long lossCount;
    private long gainCount;
}

