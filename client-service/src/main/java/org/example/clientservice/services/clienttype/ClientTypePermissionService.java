package org.example.clientservice.services.clienttype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionUpdateDTO;
import org.example.clientservice.repositories.clienttype.ClientTypePermissionRepository;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTypePermissionService implements IClientTypePermissionService {
    private final ClientTypePermissionRepository permissionRepository;
    private final ClientTypeService clientTypeService;

    @Override
    @Transactional
    public ClientTypePermission createPermission(Long clientTypeId, ClientTypePermissionCreateDTO dto) {
        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);

        if (permissionRepository.findByUserIdAndClientTypeId(dto.getUserId(), clientTypeId).isPresent()) {
            throw new ClientException("Permission already exists for user " + dto.getUserId() + " and client type " + clientTypeId);
        }

        ClientTypePermission permission = new ClientTypePermission();
        permission.setUserId(dto.getUserId());
        permission.setClientType(clientType);
        permission.setCanView(dto.getCanView() != null ? dto.getCanView() : true);
        permission.setCanCreate(dto.getCanCreate() != null ? dto.getCanCreate() : false);
        permission.setCanEdit(dto.getCanEdit() != null ? dto.getCanEdit() : false);
        permission.setCanDelete(dto.getCanDelete() != null ? dto.getCanDelete() : false);

        log.info("Creating permission for user {} and client type {}", dto.getUserId(), clientTypeId);
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public ClientTypePermission updatePermission(Long clientTypeId, Long userId, ClientTypePermissionUpdateDTO dto) {
        ClientTypePermission permission = getPermission(clientTypeId, userId);

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

        log.info("Updating permission for user {} and client type {}", userId, clientTypeId);
        return permissionRepository.save(permission);
    }

    @Override
    public ClientTypePermission getPermission(Long clientTypeId, Long userId) {
        return permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId)
                .orElseThrow(() -> new ClientNotFoundException("Permission not found for user " + userId + " and client type " + clientTypeId));
    }

    @Override
    public List<ClientTypePermission> getPermissionsByClientTypeId(Long clientTypeId) {
        return permissionRepository.findByClientTypeId(clientTypeId);
    }

    @Override
    public List<ClientTypePermission> getPermissionsByUserId(Long userId) {
        return permissionRepository.findByUserId(userId);
    }

    @Override
    public boolean canUserView(Long userId, Long clientTypeId) {
        return permissionRepository.findViewPermission(userId, clientTypeId).isPresent();
    }

    @Override
    public boolean canUserCreate(Long userId, Long clientTypeId) {
        ClientTypePermission permission = permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId).orElse(null);
        return permission != null && Boolean.TRUE.equals(permission.getCanCreate());
    }

    @Override
    public boolean canUserEdit(Long userId, Long clientTypeId) {
        ClientTypePermission permission = permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId).orElse(null);
        return permission != null && Boolean.TRUE.equals(permission.getCanEdit());
    }

    @Override
    public boolean canUserDelete(Long userId, Long clientTypeId) {
        ClientTypePermission permission = permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId).orElse(null);
        return permission != null && Boolean.TRUE.equals(permission.getCanDelete());
    }

    public ClientTypePermission getUserPermissions(Long userId, Long clientTypeId) {
        return permissionRepository.findByUserIdAndClientTypeId(userId, clientTypeId).orElse(null);
    }

    @Override
    @Transactional
    public void deletePermission(Long clientTypeId, Long userId) {
        ClientTypePermission permission = getPermission(clientTypeId, userId);
        log.info("Deleting permission for user {} and client type {}", userId, clientTypeId);
        permissionRepository.delete(permission);
    }
}

