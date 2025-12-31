package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionUpdateDTO;

import java.util.List;

public interface IClientTypePermissionService {
    @NonNull
    ClientTypePermission createPermission(@NonNull Long clientTypeId, @NonNull ClientTypePermissionCreateDTO dto);
    
    @NonNull
    ClientTypePermission updatePermission(@NonNull Long clientTypeId, @NonNull Long userId, @NonNull ClientTypePermissionUpdateDTO dto);
    
    @NonNull
    ClientTypePermission getPermission(@NonNull Long clientTypeId, @NonNull Long userId);
    
    @NonNull
    List<ClientTypePermission> getPermissionsByClientTypeId(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypePermission> getPermissionsByUserId(@NonNull Long userId);
    
    @NonNull
    List<ClientTypePermissionDTO> getMyPermissions();
    
    boolean canUserView(@NonNull Long userId, @NonNull Long clientTypeId);
    
    boolean canUserCreate(@NonNull Long userId, @NonNull Long clientTypeId);
    
    boolean canUserEdit(@NonNull Long userId, @NonNull Long clientTypeId);

    ClientTypePermission getUserPermissions(@NonNull Long userId, @NonNull Long clientTypeId);
    
    void deletePermission(@NonNull Long clientTypeId, @NonNull Long userId);
}

