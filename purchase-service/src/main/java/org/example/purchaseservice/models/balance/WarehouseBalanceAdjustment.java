package org.example.purchaseservice.models.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "warehouse_balance_adjustments")
public class WarehouseBalanceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "previous_quantity", precision = 20, scale = 2, nullable = false)
    private BigDecimal previousQuantity;

    @Column(name = "new_quantity", precision = 20, scale = 2, nullable = false)
    private BigDecimal newQuantity;

    @Column(name = "previous_total_cost", precision = 20, scale = 6, nullable = false)
    private BigDecimal previousTotalCostUah;

    @Column(name = "new_total_cost", precision = 20, scale = 6, nullable = false)
    private BigDecimal newTotalCostUah;

    @Column(name = "previous_average_price", precision = 20, scale = 6, nullable = false)
    private BigDecimal previousAveragePriceUah;

    @Column(name = "new_average_price", precision = 20, scale = 6, nullable = false)
    private BigDecimal newAveragePriceUah;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", length = 20, nullable = false)
    private AdjustmentType adjustmentType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AdjustmentType {
        QUANTITY,
        TOTAL_COST,
        BOTH
    }
}
