package org.example.clientservice.models.clienttype;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
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
@Table(name = "client_type_fields")
public class ClientTypeField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_type_id", nullable = false)
    private ClientType clientType;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_searchable")
    private Boolean isSearchable = true;

    @Column(name = "is_filterable")
    private Boolean isFilterable = false;

    @Column(name = "is_visible_in_table")
    private Boolean isVisibleInTable = true;

    @Column(name = "is_visible_in_create")
    private Boolean isVisibleInCreate = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "validation_pattern", length = 500)
    private String validationPattern;

    @Column(name = "allow_multiple")
    private Boolean allowMultiple = false;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientTypeFieldListValue> listValues = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

