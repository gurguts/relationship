package org.example.clientservice.models.field;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sources")
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "name")
    private String name;
}
