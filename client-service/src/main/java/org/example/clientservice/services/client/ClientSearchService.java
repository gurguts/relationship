package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.field.*;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.models.client.FilterIds;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSearchService implements IClientSearchService {
    private final ClientRepository clientRepository;
    private final ISourceService sourceService;
    private final IClientTypePermissionService clientTypePermissionService;

    @Override
    public Page<Client> searchClients(String query, Pageable pageable, Map<String, List<String>> filterParams,
                                      Long clientTypeId) {
        validateQuery(query);
        validateFilterParams(filterParams);

        if (query == null || query.trim().isEmpty()) {
            return fetchClients(null, filterParams, null, pageable, clientTypeId);
        }

        FilterIds filterIds = fetchFilterIds(query);
        return fetchClients(query, filterParams, filterIds, pageable, clientTypeId);
    }

    @Override
    public List<Client> searchClientsForPurchase(String query, Map<String, List<String>> filterParams) {
        log.info("Searching clients for purchase-service with query: {}, filters: {}", query, filterParams);

        FilterIds filterIds = query != null && !query.trim().isEmpty() ? fetchFilterIds(query) : null;
        return fetchClients(query, filterParams, filterIds);
    }

    @Override
    public List<Map<Long, String>> searchIdsClient(List<Long> ids) {
        List<Client> clients = clientRepository.findAllById(ids);

        return clients.stream()
                .map(client -> Map.of(client.getId(), client.getCompany()))
                .toList();
    }

    private void validateQuery(String query) {
        if (query != null && query.length() > 255) {
            throw new ClientException("Search query cannot exceed 255 characters");
        }
    }

    private void validateFilterParams(Map<String, List<String>> filterParams) {
        if (filterParams == null) {
            return;
        }
        // Валидация фильтров не требуется здесь, так как:
        // 1. Стандартные фильтры обрабатываются в ClientSpecification
        // 2. Динамические поля также обрабатываются в ClientSpecification
        // 3. Невалидные фильтры просто не найдут совпадений в базе данных
        // Если нужна строгая валидация, она должна быть на уровне контроллера или DTO
    }

    private FilterIds fetchFilterIds(String query) {
        List<Source> sourceData = sourceService.findByNameContaining(query);
        List<Long> sourceIds = sourceData.stream().map(Source::getId).collect(Collectors.toList());

        return new FilterIds(sourceData, sourceIds);
    }

    private Page<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      Pageable pageable, Long clientTypeId) {
        // Получаем доступные типы клиентов для текущего пользователя
        Long userId = getCurrentUserId();
        List<Long> allowedClientTypeIds = null;
        
        if (userId != null && !isAdmin()) {
            // Если пользователь не администратор, фильтруем по доступным типам
            allowedClientTypeIds = getAccessibleClientTypeIds(userId);
            if (allowedClientTypeIds.isEmpty()) {
                // Если у пользователя нет доступа ни к одному типу, возвращаем пустую страницу
                return Page.empty(pageable);
            }
        }
        
        // Если указан конкретный clientTypeId, проверяем доступ
        if (clientTypeId != null && userId != null && !isAdmin()) {
            if (!clientTypePermissionService.canUserView(userId, clientTypeId)) {
                // Если у пользователя нет доступа к указанному типу, возвращаем пустую страницу
                return Page.empty(pageable);
            }
        }
        
        Page<Client> clientPage = clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds != null ? filterIds.sourceIds() : null,
                clientTypeId,
                allowedClientTypeIds
        ), pageable);

        return clientPage;
    }

    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds) {
        Long clientTypeId = filterParams != null && filterParams.containsKey("clientTypeId") 
            ? Long.parseLong(filterParams.get("clientTypeId").get(0)) 
            : null;

        List<Client> clients = clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds != null ? filterIds.sourceIds() : null,
                clientTypeId
        ));

        return clients;
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            return null;
        }
        return authentication.getDetails() instanceof Long ? 
                (Long) authentication.getDetails() : null;
    }
    
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> "system:admin".equals(auth) || "administration:view".equals(auth));
    }
    
    private List<Long> getAccessibleClientTypeIds(Long userId) {
        List<ClientTypePermission> permissions = clientTypePermissionService.getPermissionsByUserId(userId);
        return permissions.stream()
                .filter(perm -> Boolean.TRUE.equals(perm.getCanView()))
                .map(perm -> perm.getClientType().getId())
                .collect(Collectors.toList());
    }
}
