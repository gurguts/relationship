package org.example.businessservice.models;

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

    public Business() {}
}