package org.example.balanceservcie.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "balances")
public class Balance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance_uah", nullable = false)
    private BigDecimal balanceUAH = BigDecimal.ZERO;

    @Column(name = "balance_eur", nullable = false)
    private BigDecimal balanceEUR = BigDecimal.ZERO;

    @Column(name = "balance_usd", nullable = false)
    private BigDecimal balanceUSD = BigDecimal.ZERO;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
