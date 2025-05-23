package org.example.clientservice.models.field;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, name = "name")
    private String name;

    public Route(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Route() {
    }
}
