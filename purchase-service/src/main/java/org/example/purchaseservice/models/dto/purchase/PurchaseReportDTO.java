package org.example.purchaseservice.models.dto.purchase;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PurchaseReportDTO {
    private Map<Long, Double> totalCollectedByProduct; // productId -> quantity
    private Map<Long, Double> totalDeliveredByProduct; // productId -> quantity (new field)
    private Map<String, Map<Long, Double>> byAttractors; // attractorName -> (productId -> quantity)
    private Map<String, Map<Long, Double>> byDrivers; // driverName -> (productId -> quantity)
    private Map<String, Double> totalSpentByCurrency; // currency -> amount
    private Map<String, Double> averagePriceByCurrency; // currency -> price
    private Map<Long, Double> averageCollectedPerTimeByProduct; // productId -> quantity
}