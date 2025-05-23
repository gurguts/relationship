package org.example.clientservice.models.field;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "businesses")
public class Business {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, name = "name")
    private String name;

    public Business(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Business(String name) {
        this.name = name;
    }

    public Business() {
    }
}