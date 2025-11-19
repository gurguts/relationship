package org.example.purchaseservice.models.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WithdrawalDTO {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long userId;
    private WithdrawalReason withdrawalReason;
    private Double quantity;
    private BigDecimal unitPriceUah;
    private BigDecimal totalCostUah;
    private String description;
    private LocalDate withdrawalDate;
    private LocalDateTime createdAt;
}
