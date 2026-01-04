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
@Table(name = "vehicle_expenses")
public class VehicleExpense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;
    
    @Column(name = "from_account_id")
    private Long fromAccountId;
    
    @Column(name = "category_id")
    private Long categoryId;
    
    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency;
    
    @Column(name = "exchange_rate", precision = 20, scale = 6)
    private BigDecimal exchangeRate;
    
    @Column(name = "converted_amount", precision = 20, scale = 2)
    private BigDecimal convertedAmount;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
