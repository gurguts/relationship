package org.example.purchaseservice.models.dto.purchase;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PurchaseReportDTO {
    private Map<Long, Double> totalCollectedByProduct;
    private Map<Long, Double> totalDeliveredByProduct;
    private Map<String, Map<Long, Double>> byAttractors;
    private Map<String, Map<Long, Double>> byDrivers;
    private Map<String, Double> totalSpentByCurrency;
    private Map<String, Double> averagePriceByCurrency;
    private Map<Long, Double> averageCollectedPerTimeByProduct;
}