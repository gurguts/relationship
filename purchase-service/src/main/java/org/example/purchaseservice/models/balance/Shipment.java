package org.example.purchaseservice.models.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "shipments")
public class Shipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "shipment_date", nullable = false)
    private LocalDate shipmentDate;
    
    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;
    
    @Column(name = "invoice_ua", length = 100)
    private String invoiceUa;
    
    @Column(name = "invoice_eu", length = 100)
    private String invoiceEu;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "total_cost_uah", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCostUah = BigDecimal.ZERO;
    
    @Column(name = "user_id")
    private Long userId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Adds withdrawal cost to total vehicle cost
     */
    public void addWithdrawalCost(BigDecimal withdrawalCost) {
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            this.totalCostUah = this.totalCostUah.add(withdrawalCost);
        }
    }
    
    /**
     * Subtracts withdrawal cost from total vehicle cost (when withdrawal is deleted)
     */
    public void subtractWithdrawalCost(BigDecimal withdrawalCost) {
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            this.totalCostUah = this.totalCostUah.subtract(withdrawalCost);
            if (this.totalCostUah.compareTo(BigDecimal.ZERO) < 0) {
                this.totalCostUah = BigDecimal.ZERO;
            }
        }
    }
}

