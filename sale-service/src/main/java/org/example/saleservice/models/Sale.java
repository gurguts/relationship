package org.example.saleservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Entity
@Table(name = "sales")
public class Sale {
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

    @Column(name = "unit_price", nullable = false, precision = 20, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "currency")
    private String currency;

    @Column(name = "transaction_id")
    private Long transaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void calculateAndSetUnitPrice() {
        if (quantity != null && totalPrice != null && quantity.compareTo(BigDecimal.ZERO) != 0) {
            this.unitPrice = totalPrice.divide(quantity, 2, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = BigDecimal.ZERO;
        }
    }
}
