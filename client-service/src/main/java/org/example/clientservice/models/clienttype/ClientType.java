package org.example.clientservice.models.clienttype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
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
    
    private static final String DEFAULT_NAME_FIELD_LABEL = "Компанія";
    private static final boolean DEFAULT_IS_ACTIVE = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(nullable = false, unique = true)
    private String name;

    @NonNull
    @Column(name = "name_field_label", nullable = false)
    private String nameFieldLabel = DEFAULT_NAME_FIELD_LABEL;

    @Column(name = "is_active")
    private Boolean isActive = DEFAULT_IS_ACTIVE;

    @Column(name = "static_fields_config", columnDefinition = "TEXT")
    private String staticFieldsConfig;

    @OneToMany(mappedBy = "clientType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientTypeField> fields;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ClientType() {
        this.fields = new ArrayList<>();
    }
}

