package org.example.saleservice.models.dto.fields;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SaleReportDTO {
    private double totalCollected;
    private Map<String, Double> byAttractors;
    private Map<String, Double> byDrivers;
    private Map<String, Double> byRegions;
    private double totalSpent;
    private double averagePrice;
    private double averageCollectedPerTime;
}
