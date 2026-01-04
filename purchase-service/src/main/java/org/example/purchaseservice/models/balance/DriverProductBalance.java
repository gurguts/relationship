package org.example.purchaseservice.models.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "driver_product_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"driver_id", "product_id"}))
public class DriverProductBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "driver_id", nullable = false)
    private Long driverId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;
    
    @Column(name = "average_price_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal averagePriceEur = BigDecimal.ZERO;
    
    @Column(name = "total_cost_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCostEur = BigDecimal.ZERO;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

