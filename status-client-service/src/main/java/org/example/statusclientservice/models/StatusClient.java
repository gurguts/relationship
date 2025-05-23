package org.example.statusclientservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "status")
public class StatusClient {
    @Id
    private Long id;
    @Column(nullable = false, name = "name")
    private String name;
}
