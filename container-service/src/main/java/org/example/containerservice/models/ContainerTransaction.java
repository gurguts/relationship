package org.example.containerservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "container_transactions")
public class ContainerTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "client_id")
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ContainerTransactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
