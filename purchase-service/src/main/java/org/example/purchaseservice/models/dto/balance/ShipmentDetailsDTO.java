package org.example.purchaseservice.models.dto.balance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShipmentDetailsDTO {
    private Long id;
    private LocalDate shipmentDate;
    private String vehicleNumber;
    private String invoiceUa;
    private String invoiceEu;
    private String description;
    private BigDecimal totalCostUah;
    private Long userId;
    private LocalDateTime createdAt;
    private List<ShipmentItemDTO> items;
    
    @Data
    @Builder
    public static class ShipmentItemDTO {
        private Long withdrawalId;
        private Long productId;
        private String productName;
        private Long warehouseId;
        private BigDecimal quantity;
        private BigDecimal unitPriceUah;
        private BigDecimal totalCostUah;
        private LocalDate withdrawalDate;
    }
}

