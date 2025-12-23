package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.UserClient;
import org.example.clientservice.clients.PurchaseClient;
import org.example.clientservice.clients.ContainerClient;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.IClientCrudService;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCrudService implements IClientCrudService {
    private final ClientRepository clientRepository;
    private final ISourceService sourceService;
    private final UserClient userCLient;
    private final IClientTypePermissionService clientTypePermissionService;
    private final PurchaseClient purchaseClient;
    private final ContainerClient containerClient;

    @Override
    @Transactional
    public Client createClient(Client client) {
        log.info("Creating client: {}", client.getCompany());

        if (client.getClientType() != null && client.getClientType().getId() != null) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null && !SecurityUtils.isAdmin()) {
                if (!clientTypePermissionService.canUserCreate(userId, client.getClientType().getId())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to create clients of this type");
                }
            }
        }
        
        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(Client client, Long id) {
        Client existingClient = getClient(id);

        if (existingClient.getClientType() != null && existingClient.getClientType().getId() != null) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null && !SecurityUtils.isAdmin()) {
                if (!clientTypePermissionService.canUserEdit(userId, existingClient.getClientType().getId())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to edit clients of this type");
                }
            }
        }

        String fullName = getFullName();

        Source clientSource = null;
        if (existingClient.getSource() != null) {
            clientSource = sourceService.getSource(existingClient.getSource());
        }

        checkSourceBasedPermission(existingClient, clientSource, "edit");
        updateExistingClient(existingClient, client, fullName, clientSource);

        log.info("Updating client with ID: {}", id);

        return clientRepository.save(existingClient);
    }

    private String getFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();

        return userCLient.getUserFullNameFromLogin(login);
    }

    @Override
    public Client getClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

        if (client.getClientType() != null && client.getClientType().getId() != null) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null && !SecurityUtils.isAdmin()) {
                if (!clientTypePermissionService.canUserView(userId, client.getClientType().getId())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to view clients of this type");
                }
            }
        }
        
        return client;
    }

    @Override
    @Transactional
    public void fullDeleteClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

        if (client.getClientType() != null && client.getClientType().getId() != null) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null && !SecurityUtils.isAdmin()) {
                org.example.clientservice.models.clienttype.ClientTypePermission permission = 
                    clientTypePermissionService.getUserPermissions(userId, client.getClientType().getId());
                if (permission == null || !Boolean.TRUE.equals(permission.getCanView())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to view clients of this type");
                }
                if (!Boolean.TRUE.equals(permission.getCanDelete())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to delete clients of this type");
                }
            }
        }

        Source clientSource = null;
        if (client.getSource() != null) {
            clientSource = sourceService.getSource(client.getSource());
        }
        checkSourceBasedPermission(client, clientSource, "delete");
        
        // Проверяем наличие связанных закупок
        try {
            var purchasesResponse = purchaseClient.getPurchasesByClientId(clientId);
            if (purchasesResponse.getBody() != null && !purchasesResponse.getBody().isEmpty()) {
                throw new ClientException("DELETE_FORBIDDEN", 
                    "Cannot delete client because there are purchases associated with it");
            }
        } catch (Exception e) {
            // Если это наша ошибка - пробрасываем дальше
            if (e instanceof ClientException) {
                throw e;
            }
            // Если ошибка при обращении к сервису - логируем и продолжаем проверку
            log.warn("Failed to check purchases for client {}: {}", clientId, e.getMessage());
        }
        
        // Проверяем наличие связанной тары
        try {
            var containersResponse = containerClient.getClientContainers(clientId);
            if (containersResponse.getBody() != null && !containersResponse.getBody().isEmpty()) {
                throw new ClientException("DELETE_FORBIDDEN", 
                    "Cannot delete client because there are containers associated with it");
            }
        } catch (Exception e) {
            // Если это наша ошибка - пробрасываем дальше
            if (e instanceof ClientException) {
                throw e;
            }
            // Если ошибка при обращении к сервису - логируем и продолжаем проверку
            log.warn("Failed to check containers for client {}: {}", clientId, e.getMessage());
        }
        
        clientRepository.deleteById(clientId);
    }

    @Override
    @Transactional
    public void deleteClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
        
        if (client.getClientType() != null && client.getClientType().getId() != null) {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null && !SecurityUtils.isAdmin()) {
                org.example.clientservice.models.clienttype.ClientTypePermission permission = 
                    clientTypePermissionService.getUserPermissions(userId, client.getClientType().getId());
                if (permission == null || !Boolean.TRUE.equals(permission.getCanView())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to view clients of this type");
                }
                if (!Boolean.TRUE.equals(permission.getCanDelete())) {
                    throw new ClientException("ACCESS_DENIED", 
                        "You do not have permission to delete clients of this type");
                }
            }
        }

        Source clientSource = null;
        if (client.getSource() != null) {
            clientSource = sourceService.getSource(client.getSource());
        }
        checkSourceBasedPermission(client, clientSource, "delete");
        
        clientRepository.deactivateClientById(clientId);
    }

    @Override
    @Transactional
    public void activateClient(Long clientId) {
        getClient(clientId);
        clientRepository.activateClientById(clientId);
    }

    private void updateExistingClient(Client existingClient, Client updatedClient, String fullName, Source clientSource) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ClientException("AUTHENTICATION_REQUIRED", "Authentication required");
        }
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean canEditStrangers = SecurityUtils.hasAuthority("client_stranger:edit");

        // Логика проверки прав доступа:
        // 1. Если у клиента нет source - можно редактировать
        // 2. Если у клиента есть source:
        //    - Если source закреплен за текущим пользователем - можно редактировать
        //    - Если source не закреплен или закреплен за другим - можно редактировать ТОЛЬКО с правом client_stranger:edit
        boolean canEditData;
        boolean isOwnClient = false; // Является ли клиент "своим" (source закреплен за пользователем или нет source)
        
        if (clientSource == null) {
            // У клиента нет source - можно редактировать
            canEditData = true;
            isOwnClient = true;
        } else {
            // У клиента есть source
            if (clientSource.getUserId() != null && currentUserId != null && 
                currentUserId.equals(clientSource.getUserId())) {
                // Source закреплен за текущим пользователем - можно редактировать
                canEditData = true;
                isOwnClient = true;
            } else {
                // Source не закреплен или закреплен за другим - можно редактировать только с правом client_stranger:edit
                canEditData = canEditStrangers;
                isOwnClient = false;
            }
        }

        // Если нет прав на редактирование - выбрасываем исключение
        if (!canEditData) {
            throw new ClientException("ACCESS_DENIED", "You do not have permission to edit this client");
        }

        // Название клиента (company) можно редактировать только для "своих" клиентов или с правом client_stranger:edit
        if (updatedClient.getCompany() != null && !isOwnClient && !canEditStrangers) {
            throw new ClientException("ACCESS_DENIED", "You do not have permission to edit client name");
        }
        
        // Обновляем название клиента только если есть права
        if (isOwnClient || canEditStrangers) {
            if (updatedClient.getCompany() != null) {
                existingClient.setCompany(updatedClient.getCompany());
            }
        }

        // Обновляем source только если он передан и изменился
        if (updatedClient.getSource() != null && !Objects.equals(updatedClient.getSource(),
                existingClient.getSource())) {
            // Изменять source могут только пользователи с правом client_stranger:edit
            if (!canEditStrangers) {
                throw new ClientException("ACCESS_DENIED", "Only users with client_stranger:edit can update source");
            }
            existingClient.setSource(updatedClient.getSource());
        }

        // Обновляем fieldValues только если они переданы
        if (updatedClient.getFieldValues() != null) {
            existingClient.getFieldValues().clear();
            updatedClient.getFieldValues().forEach(fieldValue -> {
                fieldValue.setClient(existingClient);
                existingClient.getFieldValues().add(fieldValue);
            });
        }
        
        // Явно обновляем updatedAt, чтобы @UpdateTimestamp сработал
        // Это гарантирует, что поле обновится даже если изменены только fieldValues
        existingClient.setUpdatedAt(LocalDateTime.now());
    }
    
    /**
     * Проверяет права доступа на основе source клиента.
     * Логика:
     * 1. Если у клиента нет source - можно выполнять действие
     * 2. Если у клиента есть source и он закреплен за текущим пользователем - можно выполнять действие
     * 3. Если у клиента есть source, но он закреплен за другим - можно выполнять действие только с правом client_stranger:edit
     * 
     * @param client клиент для проверки
     * @param action действие для сообщения об ошибке ("edit" или "delete")
     */
    private void checkSourceBasedPermission(Client client, String action) {
        Source clientSource = null;
        if (client.getSource() != null) {
            clientSource = sourceService.getSource(client.getSource());
        }
        checkSourceBasedPermission(client, clientSource, action);
    }
    
    /**
     * Проверяет права доступа на основе source клиента.
     * Перегрузка метода для случаев, когда Source уже загружен.
     * 
     * @param client клиент для проверки
     * @param clientSource загруженный Source (может быть null)
     * @param action действие для сообщения об ошибке ("edit" или "delete")
     */
    private void checkSourceBasedPermission(Client client, Source clientSource, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ClientException("AUTHENTICATION_REQUIRED", "Authentication required");
        }
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean canEditStrangers = SecurityUtils.hasAuthority("client_stranger:edit");
        
        // Если у пользователя есть право редактировать чужих клиентов - разрешаем
        if (canEditStrangers) {
            return;
        }
        
        // Если у клиента нет source - можно выполнять действие
        if (clientSource == null) {
            return;
        }
        
        // У клиента есть source - проверяем, закреплен ли он за текущим пользователем
        if (clientSource.getUserId() != null && currentUserId != null && 
            currentUserId.equals(clientSource.getUserId())) {
            // Source закреплен за текущим пользователем - можно выполнять действие
            return;
        }
        
        // Source не закреплен или закреплен за другим - нет прав на выполнение действия
        String actionText = "delete".equals(action) ? "deletion" : "editing";
        throw new ClientException("ACCESS_DENIED", 
            String.format("You do not have permission for %s of this client. The client has a source assigned to another user.", actionText));
    }
}


