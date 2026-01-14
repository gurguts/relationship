package org.example.purchaseservice.models.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_discrepancies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDiscrepancy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_receipt_id", nullable = false)
    private Long warehouseReceiptId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "purchased_quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal purchasedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal receivedQuantity;

    @Column(name = "discrepancy_quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal discrepancyQuantity;

    @Column(name = "unit_price_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal unitPriceEur;

    @Column(name = "discrepancy_value_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal discrepancyValueEur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private DiscrepancyType type;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum DiscrepancyType {
        LOSS,
        GAIN
    }
}

