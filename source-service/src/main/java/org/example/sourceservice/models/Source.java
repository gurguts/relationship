package org.example.sourceservice.models;

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

/*    @Column(name = "color")
    private String color;*/
}
