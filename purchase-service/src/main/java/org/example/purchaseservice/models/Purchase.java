package org.example.purchaseservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long user;

    @Column(name = "client_id", nullable = false)
    private Long client;

    @Column(name = "source_id")
    private Long source;

    @Column(name = "product_id", nullable = false)
    private Long product;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 20, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_id")
    private Long transaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "currency")
    private String currency;

    @Column(name = "exchange_rate", precision = 20, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "total_price_eur", precision = 20, scale = 6)
    private BigDecimal totalPriceEur;

    @Column(name = "unit_price_eur", precision = 20, scale = 6)
    private BigDecimal unitPriceEur;
    
    @Column(name = "quantity_eur", precision = 20, scale = 2)
    private BigDecimal quantityEur; // Quantity converted to EUR

    public void calculateAndSetUnitPrice() {
        if (quantity != null && totalPrice != null && quantity.compareTo(BigDecimal.ZERO) != 0) {
            this.unitPrice = totalPrice.divide(quantity, 6, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = BigDecimal.ZERO;
        }
    }

    /**
     * Converts prices to EUR (quantity stays the same - it's a physical value)
     * @param exchangeRateToEur exchange rate from purchase currency to EUR (from ExchangeRateService)
     */
    public void calculateAndSetPricesInEur(BigDecimal exchangeRateToEur) {
        if (totalPrice == null || quantity == null) {
            return;
        }

        // Quantity always stays the same - it's a physical value (kg, pieces, etc.)
        this.quantityEur = quantity;

        // If currency is EUR or not specified, just copy prices
        if ("EUR".equalsIgnoreCase(currency) || currency == null) {
            this.totalPriceEur = totalPrice;
            this.unitPriceEur = unitPrice;
        } else {
            // Convert prices via exchange rate to EUR
            if (exchangeRateToEur != null && exchangeRateToEur.compareTo(BigDecimal.ZERO) > 0) {
                // Convert total price: totalPrice * exchangeRate = totalPrice in EUR
                this.totalPriceEur = totalPrice.multiply(exchangeRateToEur).setScale(6, RoundingMode.HALF_UP);
                
                // Calculate unit price in EUR: totalPriceEur / quantity
                if (quantity.compareTo(BigDecimal.ZERO) != 0) {
                    this.unitPriceEur = this.totalPriceEur.divide(quantity, 6, RoundingMode.HALF_UP);
                } else {
                    this.unitPriceEur = BigDecimal.ZERO;
                }
            } else {
                // If no exchange rate, set to original values (should not happen)
                this.totalPriceEur = totalPrice;
                this.unitPriceEur = unitPrice;
            }
        }
    }
}