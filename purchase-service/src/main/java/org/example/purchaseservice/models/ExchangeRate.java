package org.example.purchaseservice.models;

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
@Table(name = "exchange_rates")
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency; // UAH or USD
    
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency = "EUR"; // Always EUR
    
    @Column(name = "rate", nullable = false, precision = 20, scale = 6)
    private BigDecimal rate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
}


