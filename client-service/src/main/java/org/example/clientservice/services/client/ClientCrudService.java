package org.example.clientservice.services.client;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.ContainerClient;
import org.example.clientservice.clients.PurchaseClient;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.IClientCrudService;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCrudService implements IClientCrudService {

    private static final String AUTHORITY_CLIENT_STRANGER_EDIT = "client_stranger:edit";
    private static final String ACTION_EDIT = "edit";
    private static final String ACTION_DELETE = "delete";

    private final ClientRepository clientRepository;
    private final ISourceService sourceService;
    private final IClientTypePermissionService clientTypePermissionService;
    private final PurchaseClient purchaseClient;
    private final ContainerClient containerClient;

    @Override
    @Transactional
    public Client createClient(@NonNull Client client) {
        log.info("Creating client: company={}", client.getCompany());

        checkClientTypePermission(client.getClientType(), PermissionAction.CREATE);

        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(@NonNull Client client, @NonNull Long id) {
        log.info("Updating client: id={}", id);

        Client existingClient = getClient(id);
        checkClientTypePermission(existingClient.getClientType(), PermissionAction.EDIT);

        Source clientSource = loadClientSource(existingClient);
        checkSourceBasedPermission(clientSource, ACTION_EDIT);

        updateExistingClient(existingClient, client, clientSource);

        return clientRepository.save(existingClient);
    }

    @Override
    public Client getClient(@NonNull Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + clientId));

        checkClientTypePermission(client.getClientType(), PermissionAction.VIEW);

        return client;
    }

    @Override
    @Transactional
    public void fullDeleteClient(@NonNull Long clientId) {
        log.info("Full deleting client: id={}", clientId);

        Client client = getClientForDeletion(clientId);
        checkDeletionPermissions(client);
        checkRelatedEntities(clientId);

        clientRepository.deleteById(clientId);
    }

    @Override
    @Transactional
    public void deleteClient(@NonNull Long clientId) {
        log.info("Deleting client: id={}", clientId);

        Client client = getClientForDeletion(clientId);
        checkDeletionPermissions(client);

        clientRepository.deactivateClientById(clientId);
    }

    @Override
    @Transactional
    public void activateClient(@NonNull Long clientId) {
        log.info("Activating client: id={}", clientId);

        getClient(clientId);
        clientRepository.activateClientById(clientId);
    }

    private void checkClientTypePermission(org.example.clientservice.models.clienttype.ClientType clientType, PermissionAction action) {
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

    private Client getClientForDeletion(@NonNull Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + clientId));

        checkClientTypePermission(client.getClientType(), PermissionAction.DELETE);

        return client;
    }

    private void checkDeletionPermissions(@NonNull Client client) {
        Source clientSource = loadClientSource(client);
        checkSourceBasedPermission(clientSource, ACTION_DELETE);
    }

    private Source loadClientSource(@NonNull Client client) {
        if (client.getSourceId() == null) {
            return null;
        }
        return sourceService.getSource(client.getSourceId());
    }

    private void checkRelatedEntities(@NonNull Long clientId) {
        checkPurchases(clientId);
        checkContainers(clientId);
    }

    private void checkPurchases(@NonNull Long clientId) {
        try {
            ResponseEntity<List<Map<String, Object>>> purchasesResponse = purchaseClient.getPurchasesByClientId(clientId);
            if (purchasesResponse.getBody() != null && !purchasesResponse.getBody().isEmpty()) {
                throw new ClientException("DELETE_FORBIDDEN",
                        "Cannot delete client because there are purchases associated with it");
            }
        } catch (ClientException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Failed to check purchases for client {}: status={}, message={}", clientId, e.status(), e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while checking purchases for client {}: {}", clientId, e.getMessage(), e);
        }
    }

    private void checkContainers(@NonNull Long clientId) {
        try {
            ResponseEntity<List<Map<String, Object>>> containersResponse = containerClient.getClientContainers(clientId);
            if (containersResponse.getBody() != null && !containersResponse.getBody().isEmpty()) {
                throw new ClientException("DELETE_FORBIDDEN",
                        "Cannot delete client because there are containers associated with it");
            }
        } catch (ClientException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Failed to check containers for client {}: status={}, message={}", clientId, e.status(), e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while checking containers for client {}: {}", clientId, e.getMessage(), e);
        }
    }

    private void updateExistingClient(@NonNull Client existingClient, @NonNull Client updatedClient, Source clientSource) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean canEditStrangers = SecurityUtils.hasAuthority(AUTHORITY_CLIENT_STRANGER_EDIT);

        ClientEditPermissions permissions = determineEditPermissions(clientSource, currentUserId, canEditStrangers);

        if (!permissions.canEditData()) {
            throw new ClientException("ACCESS_DENIED", "You do not have permission to edit this client");
        }

        updateCompany(existingClient, updatedClient, permissions);
        updateSource(existingClient, updatedClient, canEditStrangers);
        updateFieldValues(existingClient, updatedClient);

        existingClient.setUpdatedAt(LocalDateTime.now());
    }

    private ClientEditPermissions determineEditPermissions(Source clientSource, Long currentUserId, boolean canEditStrangers) {
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

    private void updateCompany(@NonNull Client existingClient, @NonNull Client updatedClient, ClientEditPermissions permissions) {

        if (!permissions.canEditCompany()) {
            throw new ClientException("ACCESS_DENIED", "You do not have permission to edit client name");
        }

        existingClient.setCompany(updatedClient.getCompany());
    }

    private void updateSource(@NonNull Client existingClient, @NonNull Client updatedClient, boolean canEditStrangers) {
        if (updatedClient.getSourceId() == null) {
            return;
        }

        if (Objects.equals(updatedClient.getSourceId(), existingClient.getSourceId())) {
            return;
        }

        if (!canEditStrangers) {
            throw new ClientException("ACCESS_DENIED", "Only users with client_stranger:edit can update source");
        }

        existingClient.setSourceId(updatedClient.getSourceId());
    }

    private void updateFieldValues(@NonNull Client existingClient, @NonNull Client updatedClient) {
        if (updatedClient.getFieldValues() == null) {
            return;
        }

        existingClient.getFieldValues().clear();
        updatedClient.getFieldValues().forEach(fieldValue -> {
            fieldValue.setClient(existingClient);
            existingClient.getFieldValues().add(fieldValue);
        });
    }

    private void checkSourceBasedPermission(Source clientSource, @NonNull String action) {
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

    private enum PermissionAction {
        CREATE, EDIT, VIEW, DELETE
    }

    private record ClientEditPermissions(boolean canEditData, boolean canEditCompany) {
    }
}
