package org.example.purchaseservice.models.dto.balance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiclesStatsDTO {
    private BigDecimal totalQuantityKg;
    private BigDecimal totalCostEur;
}

