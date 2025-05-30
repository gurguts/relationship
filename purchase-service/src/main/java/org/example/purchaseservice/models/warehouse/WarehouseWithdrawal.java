package org.example.purchaseservice.models.warehouse;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false)
    private WithdrawalReason reasonType;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity;

    @Column(name = "description")
    private String description;

    @Column(name = "withdrawal_date", nullable = false)
    private LocalDate withdrawalDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}