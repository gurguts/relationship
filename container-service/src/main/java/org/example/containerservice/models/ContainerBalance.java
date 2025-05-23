package org.example.containerservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "container_balances")
public class ContainerBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(name = "total_quantity", nullable = false)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @Column(name = "client_quantity", nullable = false)
    private BigDecimal clientQuantity = BigDecimal.ZERO;

    @Column(name = "updated_at", updatable = false, insertable = false)
    private Timestamp updatedAt;
}
