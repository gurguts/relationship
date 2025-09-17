package org.example.purchaseservice.models.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "withdrawal_reasons")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WithdrawalReason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private Purpose purpose;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Purpose {
        ADDING, REMOVING, BOTH
    }
}
