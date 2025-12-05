package org.example.clientservice.models.clienttype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "client_types")
public class ClientType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "name_field_label", nullable = false)
    private String nameFieldLabel = "Компанія";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "static_fields_config", columnDefinition = "TEXT")
    private String staticFieldsConfig;

    @OneToMany(mappedBy = "clientType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientTypeField> fields = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

