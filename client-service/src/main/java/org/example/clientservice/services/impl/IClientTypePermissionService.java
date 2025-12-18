package org.example.clientservice.services.impl;

import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionUpdateDTO;

import java.util.List;

public interface IClientTypePermissionService {
    ClientTypePermission createPermission(Long clientTypeId, ClientTypePermissionCreateDTO dto);
    
    ClientTypePermission updatePermission(Long clientTypeId, Long userId, ClientTypePermissionUpdateDTO dto);
    
    ClientTypePermission getPermission(Long clientTypeId, Long userId);
    
    List<ClientTypePermission> getPermissionsByClientTypeId(Long clientTypeId);
    
    List<ClientTypePermission> getPermissionsByUserId(Long userId);
    
    boolean canUserView(Long userId, Long clientTypeId);
    
    boolean canUserCreate(Long userId, Long clientTypeId);
    
    boolean canUserEdit(Long userId, Long clientTypeId);
    
    boolean canUserDelete(Long userId, Long clientTypeId);
    
    ClientTypePermission getUserPermissions(Long userId, Long clientTypeId);
    
    void deletePermission(Long clientTypeId, Long userId);
}

