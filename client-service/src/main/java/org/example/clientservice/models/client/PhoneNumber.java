package org.example.clientservice.models.client;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "phone_numbers")
public class PhoneNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String number;

    @ManyToOne
    @JsonBackReference
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "client_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
    private Client client;

    @Override
    public String toString() {
        return String.format("PhoneNumber{id=%d, number='%s'}", id, number);
    }
}