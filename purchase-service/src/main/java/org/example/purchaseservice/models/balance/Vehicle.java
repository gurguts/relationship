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
@Table(name = "vehicles")
public class Vehicle {
    
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
    
    @Column(name = "total_cost_eur", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalCostEur = BigDecimal.ZERO;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "is_our_vehicle", nullable = false)
    private Boolean isOurVehicle = true;
    
    @Column(name = "sender", length = 255)
    private String sender;
    
    @Column(name = "receiver", length = 255)
    private String receiver;
    
    @Column(name = "destination_country", length = 100)
    private String destinationCountry;
    
    @Column(name = "destination_place", length = 255)
    private String destinationPlace;
    
    @Column(name = "product", length = 255)
    private String product;
    
    @Column(name = "product_quantity", length = 100)
    private String productQuantity;
    
    @Column(name = "declaration_number", length = 100)
    private String declarationNumber;
    
    @Column(name = "terminal", length = 100)
    private String terminal;
    
    @Column(name = "driver_full_name", length = 255)
    private String driverFullName;
    
    @Column(name = "eur1", nullable = false)
    private Boolean eur1 = false;
    
    @Column(name = "fito", nullable = false)
    private Boolean fito = false;
    
    @Column(name = "customs_date")
    private LocalDate customsDate;
    
    @Column(name = "customs_clearance_date")
    private LocalDate customsClearanceDate;
    
    @Column(name = "unloading_date")
    private LocalDate unloadingDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;
    
    @Column(name = "invoice_ua_date")
    private LocalDate invoiceUaDate;
    
    @Column(name = "invoice_ua_price_per_ton", precision = 20, scale = 6)
    private BigDecimal invoiceUaPricePerTon;
    
    @Column(name = "invoice_ua_total_price", precision = 20, scale = 6)
    private BigDecimal invoiceUaTotalPrice;
    
    @Column(name = "invoice_eu_date")
    private LocalDate invoiceEuDate;
    
    @Column(name = "invoice_eu_price_per_ton", precision = 20, scale = 6)
    private BigDecimal invoiceEuPricePerTon;
    
    @Column(name = "invoice_eu_total_price", precision = 20, scale = 6)
    private BigDecimal invoiceEuTotalPrice;
    
    @Column(name = "reclamation", precision = 20, scale = 6)
    private BigDecimal reclamation;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public void addWithdrawalCost(BigDecimal withdrawalCost) {
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            this.totalCostEur = this.totalCostEur.add(withdrawalCost);
        }
    }
    
    public void subtractWithdrawalCost(BigDecimal withdrawalCost) {
        if (withdrawalCost != null && withdrawalCost.compareTo(BigDecimal.ZERO) > 0) {
            this.totalCostEur = this.totalCostEur.subtract(withdrawalCost);
            if (this.totalCostEur.compareTo(BigDecimal.ZERO) < 0) {
                this.totalCostEur = BigDecimal.ZERO;
            }
        }
    }
}

