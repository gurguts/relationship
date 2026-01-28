package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.IClientCrudService;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.example.clientservice.services.client.ClientPermissionService.PermissionAction;
import static org.example.clientservice.services.client.ClientPermissionService.ClientEditPermissions;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCrudService implements IClientCrudService {

    private static final String AUTHORITY_CLIENT_STRANGER_EDIT = "client_stranger:edit";
    private static final String ACTION_EDIT = "edit";
    private static final String ACTION_DELETE = "delete";

    private final ClientRepository clientRepository;
    private final ISourceService sourceService;
    private final ClientPermissionService clientPermissionService;
    private final ClientDeletionValidator clientDeletionValidator;

    @Override
    @Transactional
    public Client createClient(@NonNull Client client) {
        log.info("Creating client: company={}", client.getCompany());

        clientPermissionService.checkClientTypePermission(client.getClientType(), PermissionAction.CREATE);

        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(@NonNull Client client, @NonNull Long id) {
        log.info("Updating client: id={}", id);

        Client existingClient = getClient(id);
        clientPermissionService.checkClientTypePermission(existingClient.getClientType(), PermissionAction.EDIT);

        Source clientSource = loadClientSource(existingClient);
        clientPermissionService.checkSourceBasedPermission(clientSource, ACTION_EDIT);

        updateExistingClient(existingClient, client, clientSource);

        return clientRepository.save(existingClient);
    }

    @Override
    public Client getClient(@NonNull Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + clientId));

        clientPermissionService.checkClientTypePermission(client.getClientType(), PermissionAction.VIEW);

        return client;
    }

    @Override
    @Transactional
    public void fullDeleteClient(@NonNull Long clientId) {
        log.info("Full deleting client: id={}", clientId);

        Client client = getClientForDeletion(clientId);
        checkDeletionPermissions(client);
        clientDeletionValidator.checkRelatedEntities(clientId);

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


    private Client getClientForDeletion(@NonNull Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + clientId));

        clientPermissionService.checkClientTypePermission(client.getClientType(), PermissionAction.DELETE);

        return client;
    }

    private void checkDeletionPermissions(@NonNull Client client) {
        Source clientSource = loadClientSource(client);
        clientPermissionService.checkSourceBasedPermission(clientSource, ACTION_DELETE);
    }

    private Source loadClientSource(@NonNull Client client) {
        if (client.getSourceId() == null) {
            return null;
        }
        return sourceService.getSource(client.getSourceId());
    }


    private void updateExistingClient(@NonNull Client existingClient, @NonNull Client updatedClient, Source clientSource) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean canEditStrangers = SecurityUtils.hasAuthority(AUTHORITY_CLIENT_STRANGER_EDIT);

        ClientEditPermissions permissions = clientPermissionService.determineEditPermissions(clientSource, currentUserId, canEditStrangers);

        if (!permissions.canEditData()) {
            throw new ClientException("ACCESS_DENIED", "You do not have permission to edit this client");
        }

        updateCompany(existingClient, updatedClient, permissions);
        updateSource(existingClient, updatedClient, canEditStrangers);
        updateFieldValues(existingClient, updatedClient);

        existingClient.setUpdatedAt(LocalDateTime.now());
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

}
