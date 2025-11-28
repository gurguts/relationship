package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientTypePermissionDTO {
    private Long id;
    private Long userId;
    private Long clientTypeId;
    private String clientTypeName;
    private Boolean canView;
    private Boolean canCreate;
    private Boolean canEdit;
    private Boolean canDelete;
    private LocalDateTime createdAt;
}

