package org.example.purchaseservice.models.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing product transfer between different products within warehouse
 * Records movement of product from one product type to another
 */
@Entity
@Table(name = "product_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductTransfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Warehouse where transfer happens
     */
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    
    /**
     * Source product (removed from)
     */
    @Column(name = "from_product_id", nullable = false)
    private Long fromProductId;
    
    /**
     * Target product (added to)
     */
    @Column(name = "to_product_id", nullable = false)
    private Long toProductId;
    
    /**
     * Quantity transferred
     */
    @Column(name = "quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity;
    
    /**
     * Unit price at time of transfer (taken from source product)
     */
    @Column(name = "unit_price_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal unitPriceEur;
    
    /**
     * Total cost of transferred product
     */
    @Column(name = "total_cost_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCostEur;
    
    /**
     * Date of transfer
     */
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;
    
    /**
     * User who performed the transfer
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Reason for transfer (references WithdrawalReason)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reason_id", nullable = false)
    private WithdrawalReason reason;
    
    /**
     * Optional description/comment
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

