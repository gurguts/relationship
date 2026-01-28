package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientPermissionService {

    private static final String AUTHORITY_CLIENT_STRANGER_EDIT = "client_stranger:edit";
    private static final String ACTION_DELETE = "delete";

    private final IClientTypePermissionService clientTypePermissionService;

    public void checkClientTypePermission(ClientType clientType, PermissionAction action) {
        if (clientType == null || clientType.getId() == null) {
            return;
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null || SecurityUtils.isAdmin()) {
            return;
        }

        boolean hasPermission = switch (action) {
            case CREATE -> clientTypePermissionService.canUserCreate(userId, clientType.getId());
            case EDIT -> clientTypePermissionService.canUserEdit(userId, clientType.getId());
            case VIEW -> clientTypePermissionService.canUserView(userId, clientType.getId());
            case DELETE -> {
                ClientTypePermission permission = clientTypePermissionService.getUserPermissions(userId, clientType.getId());
                yield permission != null && Boolean.TRUE.equals(permission.getCanView()) && Boolean.TRUE.equals(permission.getCanDelete());
            }
        };

        if (!hasPermission) {
            throw new ClientException("ACCESS_DENIED",
                    String.format("You do not have permission to %s clients of this type", action.name().toLowerCase()));
        }
    }

    public void checkSourceBasedPermission(Source clientSource, @NonNull String action) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean canEditStrangers = SecurityUtils.hasAuthority(AUTHORITY_CLIENT_STRANGER_EDIT);

        if (canEditStrangers) {
            return;
        }

        if (clientSource == null) {
            return;
        }

        boolean isOwnSource = clientSource.getUserId() != null && currentUserId != null
                && currentUserId.equals(clientSource.getUserId());

        if (isOwnSource) {
            return;
        }

        String actionText = ACTION_DELETE.equals(action) ? "deletion" : "editing";
        throw new ClientException("ACCESS_DENIED",
                String.format("You do not have permission for %s of this client. The client has a source assigned to another user.", actionText));
    }

    public ClientEditPermissions determineEditPermissions(Source clientSource, Long currentUserId, boolean canEditStrangers) {
        if (clientSource == null) {
            return new ClientEditPermissions(true, true);
        }

        boolean isOwnSource = clientSource.getUserId() != null && currentUserId != null
                && currentUserId.equals(clientSource.getUserId());

        if (isOwnSource) {
            return new ClientEditPermissions(true, true);
        }

        return new ClientEditPermissions(canEditStrangers, canEditStrangers);
    }

    public enum PermissionAction {
        CREATE, EDIT, VIEW, DELETE
    }

    public record ClientEditPermissions(boolean canEditData, boolean canEditCompany) {
    }
}
