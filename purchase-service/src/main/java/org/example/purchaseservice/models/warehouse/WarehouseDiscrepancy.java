package org.example.purchaseservice.models.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for tracking discrepancies between purchased and received quantities
 * (losses or gains when warehouse clerk receives goods from driver)
 */
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
    
    /**
     * Reference to warehouse receipt where discrepancy occurred
     */
    @Column(name = "warehouse_receipt_id", nullable = false)
    private Long warehouseReceiptId;
    
    /**
     * Driver who brought the goods
     */
    @Column(name = "driver_id", nullable = false)
    private Long driverId;
    
    /**
     * Product with discrepancy
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    /**
     * Warehouse where receipt happened
     */
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    
    /**
     * Date of the receipt
     */
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;
    
    /**
     * Quantity purchased by driver (from driver balance)
     */
    @Column(name = "purchased_quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal purchasedQuantity;
    
    /**
     * Quantity actually received by warehouse clerk
     */
    @Column(name = "received_quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal receivedQuantity;
    
    /**
     * Discrepancy quantity (positive = gain, negative = loss)
     * = receivedQuantity - purchasedQuantity
     */
    @Column(name = "discrepancy_quantity", nullable = false, precision = 20, scale = 2)
    private BigDecimal discrepancyQuantity;
    
    /**
     * Average price per unit (from driver balance)
     */
    @Column(name = "unit_price_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal unitPriceEur;
    
    /**
     * Total value of discrepancy (discrepancyQuantity * unitPriceEur)
     */
    @Column(name = "discrepancy_value_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal discrepancyValueEur;
    
    /**
     * Type of discrepancy: LOSS (negative) or GAIN (positive)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private DiscrepancyType type;
    
    /**
     * Optional comment/reason
     */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    /**
     * User who created the receipt (warehouse clerk)
     */
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum DiscrepancyType {
        LOSS,  // Received less than purchased (wastage/shortage)
        GAIN   // Received more than purchased (excess)
    }
}

