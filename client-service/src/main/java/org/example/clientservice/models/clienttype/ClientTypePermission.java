package org.example.clientservice.models.clienttype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "client_type_permissions")
public class ClientTypePermission {
    
    private static final boolean DEFAULT_CAN_VIEW = true;
    private static final boolean DEFAULT_CAN_CREATE = false;
    private static final boolean DEFAULT_CAN_EDIT = false;
    private static final boolean DEFAULT_CAN_DELETE = false;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_type_id", nullable = false)
    private ClientType clientType;

    @Column(name = "can_view")
    private Boolean canView = DEFAULT_CAN_VIEW;

    @Column(name = "can_create")
    private Boolean canCreate = DEFAULT_CAN_CREATE;

    @Column(name = "can_edit")
    private Boolean canEdit = DEFAULT_CAN_EDIT;

    @Column(name = "can_delete")
    private Boolean canDelete = DEFAULT_CAN_DELETE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

