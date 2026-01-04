package org.example.clientservice.models.clienttype;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
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
@Table(name = "client_type_fields")
public class ClientTypeField {
    
    private static final boolean DEFAULT_IS_REQUIRED = false;
    private static final boolean DEFAULT_IS_SEARCHABLE = true;
    private static final boolean DEFAULT_IS_FILTERABLE = false;
    private static final boolean DEFAULT_IS_VISIBLE_IN_TABLE = true;
    private static final boolean DEFAULT_IS_VISIBLE_IN_CREATE = true;
    private static final int DEFAULT_DISPLAY_ORDER = 0;
    private static final boolean DEFAULT_ALLOW_MULTIPLE = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_type_id", nullable = false)
    private ClientType clientType;

    @NonNull
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @NonNull
    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(name = "is_required")
    private Boolean isRequired = DEFAULT_IS_REQUIRED;

    @Column(name = "is_searchable")
    private Boolean isSearchable = DEFAULT_IS_SEARCHABLE;

    @Column(name = "is_filterable")
    private Boolean isFilterable = DEFAULT_IS_FILTERABLE;

    @Column(name = "is_visible_in_table")
    private Boolean isVisibleInTable = DEFAULT_IS_VISIBLE_IN_TABLE;

    @Column(name = "is_visible_in_create")
    private Boolean isVisibleInCreate = DEFAULT_IS_VISIBLE_IN_CREATE;

    @Column(name = "display_order")
    private Integer displayOrder = DEFAULT_DISPLAY_ORDER;

    @Column(name = "column_width")
    private Integer columnWidth;

    @Column(name = "validation_pattern", length = 500)
    private String validationPattern;

    @Column(name = "allow_multiple")
    private Boolean allowMultiple = DEFAULT_ALLOW_MULTIPLE;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<ClientTypeFieldListValue> listValues;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ClientTypeField() {
        this.listValues = new ArrayList<>();
    }
}

