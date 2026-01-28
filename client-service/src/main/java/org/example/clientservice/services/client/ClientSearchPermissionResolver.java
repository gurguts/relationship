package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientSearchPermissionResolver {

    private final IClientTypePermissionService clientTypePermissionService;

    public List<Long> determineAllowedClientTypeIds(Long clientTypeId) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null || SecurityUtils.isAdmin()) {
            return null;
        }

        List<Long> allowedClientTypeIds = getAccessibleClientTypeIds(userId);

        if (allowedClientTypeIds.isEmpty()) {
            return Collections.emptyList();
        }

        if (clientTypeId != null && !allowedClientTypeIds.contains(clientTypeId)) {
            return Collections.emptyList();
        }

        return allowedClientTypeIds;
    }

    private List<Long> getAccessibleClientTypeIds(@NonNull Long userId) {
        List<ClientTypePermission> permissions = clientTypePermissionService.getPermissionsByUserId(userId);
        return permissions.stream()
                .filter(perm -> perm != null && Boolean.TRUE.equals(perm.getCanView()))
                .map(ClientTypePermission::getClientType)
                .map(ClientType::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
