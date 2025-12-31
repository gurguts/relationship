package org.example.clientservice.mappers.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionDTO;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ClientTypePermissionMapper {

    public ClientTypePermissionDTO toDTO(@NonNull ClientTypePermission permission) {
        ClientTypePermissionDTO dto = new ClientTypePermissionDTO();
        dto.setId(permission.getId());
        dto.setUserId(permission.getUserId());
        
        if (permission.getClientType() != null) {
            dto.setClientTypeId(permission.getClientType().getId());
            dto.setClientTypeName(permission.getClientType().getName());
        }
        
        dto.setCanView(permission.getCanView());
        dto.setCanCreate(permission.getCanCreate());
        dto.setCanEdit(permission.getCanEdit());
        dto.setCanDelete(permission.getCanDelete());
        dto.setCreatedAt(permission.getCreatedAt());
        return dto;
    }
}

