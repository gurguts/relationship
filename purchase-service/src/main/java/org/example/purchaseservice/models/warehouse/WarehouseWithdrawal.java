package org.example.purchaseservice.models.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "warehouse_withdrawals")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WarehouseWithdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "withdrawal_reason_id", nullable = false)
    private WithdrawalReason withdrawalReason;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price_eur", precision = 20, scale = 6)
    private BigDecimal unitPriceEur;

    @Column(name = "total_cost_eur", precision = 20, scale = 6)
    private BigDecimal totalCostEur;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "description")
    private String description;

    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}