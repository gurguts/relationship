package org.example.purchaseservice.models.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseReportDTO {
    private List<DriverReport> drivers;
    private List<SourceReport> sources;
    private List<ProductTotal> totals;

    @Data
    public static class DriverReport {
        private Long userId;
        private String userName;
        private List<ProductInfo> products;
    }

    @Data
    public static class SourceReport {
        private Long sourceId;
        private String sourceName;
        private List<ProductInfo> products;
    }

    @Data
    public static class ProductInfo {
        private Long productId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal totalPriceEur;
    }

    @Data
    public static class ProductTotal {
        private Long productId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal totalPriceEur;
    }
}
