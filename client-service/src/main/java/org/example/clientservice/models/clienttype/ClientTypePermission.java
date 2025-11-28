package org.example.clientservice.models.clienttype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "client_type_permissions")
public class ClientTypePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_type_id", nullable = false)
    private ClientType clientType;

    @Column(name = "can_view")
    private Boolean canView = true;

    @Column(name = "can_create")
    private Boolean canCreate = false;

    @Column(name = "can_edit")
    private Boolean canEdit = false;

    @Column(name = "can_delete")
    private Boolean canDelete = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

