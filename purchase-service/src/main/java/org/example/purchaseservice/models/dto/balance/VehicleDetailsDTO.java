package org.example.purchaseservice.models.dto.balance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VehicleDetailsDTO {
    private Long id;
    private LocalDate shipmentDate;
    private String vehicleNumber;
    private String invoiceUa;
    private String invoiceEu;
    private String description;
    private BigDecimal totalCostEur;
    private Long userId;
    private LocalDateTime createdAt;
    private List<VehicleItemDTO> items;
    
    @Data
    @Builder
    public static class VehicleItemDTO {
        private Long withdrawalId;
        private Long productId;
        private String productName;
        private Long warehouseId;
        private BigDecimal quantity;
        private BigDecimal unitPriceEur;
        private BigDecimal totalCostEur;
        private LocalDate withdrawalDate;
    }
}

