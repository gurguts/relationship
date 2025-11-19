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
@Table(name = "shipment_products")
public class ShipmentProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;
    
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity;
    
    @Column(name = "unit_price_uah", nullable = false, precision = 20, scale = 6)
    private BigDecimal unitPriceUah;
    
    @Column(name = "total_cost_uah", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCostUah;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}

