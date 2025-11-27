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
@Table(name = "warehouse_receipts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WarehouseReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "executor_user_id", nullable = false)
    private Long executorUserId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity;
    
    @Column(name = "driver_balance_quantity", precision = 20, scale = 2)
    private BigDecimal driverBalanceQuantity;

    @Column(name = "unit_price_eur", precision = 20, scale = 6)
    private BigDecimal unitPriceEur;

    @Column(name = "total_cost_eur", precision = 20, scale = 6)
    private BigDecimal totalCostEur;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private WithdrawalReason type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

