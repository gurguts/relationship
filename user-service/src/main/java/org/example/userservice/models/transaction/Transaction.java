package org.example.userservice.models.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id")
    private Long toAccountId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "executor_user_id")
    private Long executorUserId;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "exchange_rate", precision = 20, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "converted_currency", length = 3)
    private String convertedCurrency;

    @Column(name = "converted_amount", precision = 20, scale = 2)
    private BigDecimal convertedAmount;
}
