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
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private Long destinationCountryId;
    private String destinationCountryName;
    private Long destinationPlaceId;
    private String destinationPlaceName;
    private String product;
    private String productQuantity;
    private String declarationNumber;
    private Long terminalId;
    private String terminalName;
    private String driverFullName;
    private Boolean isOurVehicle;
    private Boolean eur1;
    private Boolean fito;
    private LocalDate customsDate;
    private LocalDate customsClearanceDate;
    private LocalDate unloadingDate;
    private LocalDate invoiceUaDate;
    private BigDecimal invoiceUaPricePerTon;
    private BigDecimal invoiceUaTotalPrice;
    private LocalDate invoiceEuDate;
    private BigDecimal invoiceEuPricePerTon;
    private BigDecimal invoiceEuTotalPrice;
    private BigDecimal reclamation;
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;
    private BigDecimal margin;
    private CarrierDetailsDTO carrier;
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

