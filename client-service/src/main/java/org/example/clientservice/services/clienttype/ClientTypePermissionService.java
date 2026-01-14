package org.example.clientservice.services.clienttype;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.mappers.clienttype.ClientTypePermissionMapper;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionUpdateDTO;
import org.example.clientservice.repositories.clienttype.ClientTypePermissionRepository;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.example.clientservice.services.impl.IClientTypeService;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTypePermissionService implements IClientTypePermissionService {
    private static final boolean DEFAULT_CAN_VIEW = true;
    private static final boolean DEFAULT_CAN_CREATE = false;
    private static final boolean DEFAULT_CAN_EDIT = false;
    private static final boolean DEFAULT_CAN_DELETE = false;
    
    private final ClientTypePermissionRepository permissionRepository;
    private final IClientTypeService clientTypeService;
    private final ClientTypePermissionMapper permissionMapper;

    @Override
    @Transactional
    @NonNull
    public ClientTypePermission createPermission(@NonNull Long clientTypeId, @NonNull ClientTypePermissionCreateDTO dto) {
        log.info("Creating permission for user {} and client type {}", dto.getUserId(), clientTypeId);
        
        try {
            validateCreatePermissionRequest(clientTypeId, dto);
            
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);

            if (permissionRepository.findByUserIdAndClientTypeId(dto.getUserId(), clientTypeId).isPresent()) {
                throw new ClientException("PERMISSION_ALREADY_EXISTS", 
                    String.format("Permission already exists for user %d and client type %d", dto.getUserId(), clientTypeId));
            }

            ClientTypePermission permission = buildPermission(dto, clientType);
            return permissionRepository.save(permission);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating permission for user {} and client type {}: {}", 
                    dto.getUserId(), clientTypeId, e.getMessage(), e);
            throw new ClientException("PERMISSION_CREATION_ERROR",
                    String.format("Failed to create permission: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    @NonNull
    public ClientTypePermission updatePermission(@NonNull Long clientTypeId, @NonNull Long userId, 
                                                 @NonNull ClientTypePermissionUpdateDTO dto) {
        log.info("Updating permission for user {} and client type {}", userId, clientTypeId);
        
        try {
            ClientTypePermission permission = getPermission(clientTypeId, userId);
            updatePermissionFields(permission, dto);
            return permissionRepository.save(permission);
        } catch (ClientNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating permission for user {} and client type {}: {}", 
                    userId, clientTypeId, e.getMessage(), e);
            throw new ClientException("PERMISSION_UPDATE_ERROR",
                    String.format("Failed to update permission: %s", e.getMessage()), e);
        }
    }

    @Override
    @NonNull
    public ClientTypePermission getPermission(@NonNull Long clientTypeId, @NonNull Long userId) {
        return permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId)
                .orElseThrow(() -> new ClientNotFoundException(
                        String.format("Permission not found for user %d and client type %d", userId, clientTypeId)));
    }

    @Override
    @NonNull
    public List<ClientTypePermission> getPermissionsByClientTypeId(@NonNull Long clientTypeId) {
        return permissionRepository.findByClientTypeId(clientTypeId);
    }

    @Override
    @NonNull
    public List<ClientTypePermission> getPermissionsByUserId(@NonNull Long userId) {
        return permissionRepository.findByUserId(userId);
    }

    @Override
    public boolean canUserView(@NonNull Long userId, @NonNull Long clientTypeId) {
        try {
            return permissionRepository.findViewPermission(userId, clientTypeId).isPresent();
        } catch (Exception e) {
            log.warn("Error checking view permission for user {} and client type {}: {}", 
                    userId, clientTypeId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canUserCreate(@NonNull Long userId, @NonNull Long clientTypeId) {
        return checkUserPermission(userId, clientTypeId, ClientTypePermission::getCanCreate);
    }

    @Override
    public boolean canUserEdit(@NonNull Long userId, @NonNull Long clientTypeId) {
        return checkUserPermission(userId, clientTypeId, ClientTypePermission::getCanEdit);
    }

    @Override
    public ClientTypePermission getUserPermissions(@NonNull Long userId, @NonNull Long clientTypeId) {
        try {
            return permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId).orElse(null);
        } catch (Exception e) {
            log.warn("Error getting user permissions for user {} and client type {}: {}", 
                    userId, clientTypeId, e.getMessage());
            return null;
        }
    }

    @Override
    @NonNull
    public List<ClientTypePermissionDTO> getMyPermissions() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ClientException("UNAUTHORIZED", "User not authenticated");
        }
        
        List<ClientTypePermission> permissions = getPermissionsByUserId(userId);
        
        return permissions.stream()
                .map(permissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePermission(@NonNull Long clientTypeId, @NonNull Long userId) {
        log.info("Deleting permission for user {} and client type {}", userId, clientTypeId);
        
        try {
            ClientTypePermission permission = getPermission(clientTypeId, userId);
            permissionRepository.delete(permission);
        } catch (ClientNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting permission for user {} and client type {}: {}", 
                    userId, clientTypeId, e.getMessage(), e);
            throw new ClientException("PERMISSION_DELETION_ERROR",
                    String.format("Failed to delete permission: %s", e.getMessage()), e);
        }
    }
    
    private void validateCreatePermissionRequest(@NonNull Long clientTypeId, @NonNull ClientTypePermissionCreateDTO dto) {
        if (dto.getUserId() == null) {
            throw new ClientException("INVALID_USER_ID", "User ID cannot be null");
        }
        
        if (clientTypeId <= 0) {
            throw new ClientException("INVALID_CLIENT_TYPE_ID", "Client type ID must be positive");
        }
        
        if (dto.getUserId() <= 0) {
            throw new ClientException("INVALID_USER_ID", "User ID must be positive");
        }
    }
    
    private ClientTypePermission buildPermission(@NonNull ClientTypePermissionCreateDTO dto, @NonNull ClientType clientType) {
        ClientTypePermission permission = new ClientTypePermission();
        permission.setUserId(dto.getUserId());
        permission.setClientType(clientType);
        permission.setCanView(dto.getCanView() != null ? dto.getCanView() : DEFAULT_CAN_VIEW);
        permission.setCanCreate(dto.getCanCreate() != null ? dto.getCanCreate() : DEFAULT_CAN_CREATE);
        permission.setCanEdit(dto.getCanEdit() != null ? dto.getCanEdit() : DEFAULT_CAN_EDIT);
        permission.setCanDelete(dto.getCanDelete() != null ? dto.getCanDelete() : DEFAULT_CAN_DELETE);
        return permission;
    }
    
    private void updatePermissionFields(@NonNull ClientTypePermission permission, 
                                       @NonNull ClientTypePermissionUpdateDTO dto) {
        if (dto.getCanView() != null) {
            permission.setCanView(dto.getCanView());
        }
        if (dto.getCanCreate() != null) {
            permission.setCanCreate(dto.getCanCreate());
        }
        if (dto.getCanEdit() != null) {
            permission.setCanEdit(dto.getCanEdit());
        }
        if (dto.getCanDelete() != null) {
            permission.setCanDelete(dto.getCanDelete());
        }
    }
    
    private boolean checkUserPermission(@NonNull Long userId, @NonNull Long clientTypeId,
                                        @NonNull Function<ClientTypePermission, Boolean> permissionGetter) {
        try {
            ClientTypePermission permission = getUserPermissions(userId, clientTypeId);
            return permission != null && Boolean.TRUE.equals(permissionGetter.apply(permission));
        } catch (Exception e) {
            log.warn("Error checking permission for user {} and client type {}: {}", 
                    userId, clientTypeId, e.getMessage());
            return false;
        }
    }
}
