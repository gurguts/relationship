package org.example.containerservice.services;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.clients.ClientApiClient;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.ClientData;
import org.example.containerservice.models.dto.PageResponse;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.example.containerservice.models.dto.container.ClientContainerResponseDTO;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.spec.ClientContainerSpecification;
import org.example.containerservice.utils.FilterUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.example.containerservice.services.impl.IClientContainerSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerSearchService implements IClientContainerSearchService {

    private static final String FILTER_KEY_SOURCE = "source";
    private static final String FILTER_KEY_CLIENT_SOURCE = "clientSource";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_FROM = "clientCreatedAtFrom";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_TO = "clientCreatedAtTo";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_FROM = "clientUpdatedAtFrom";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_TO = "clientUpdatedAtTo";
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final String FILTER_PREFIX_FIELD = "field";
    private static final String MAPPED_KEY_SOURCE = "source";
    private static final String MAPPED_KEY_CREATED_AT_FROM = "createdAtFrom";
    private static final String MAPPED_KEY_CREATED_AT_TO = "createdAtTo";
    private static final String MAPPED_KEY_UPDATED_AT_FROM = "updatedAtFrom";
    private static final String MAPPED_KEY_UPDATED_AT_TO = "updatedAtTo";

    private final ClientContainerRepository clientContainerRepository;
    private final ClientApiClient clientApiClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientContainerResponseDTO> searchClientContainer(String query,
                                                                          @NonNull Pageable pageable,
                                                                          Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = FilterUtils.filterClientParams(filterParams);
        
        ClientData clientData = fetchClientData(query, clientFilterParams, clientTypeId);
        List<Long> clientIds = clientData.clientIds();

        Page<ClientContainer> clientContainerPage = fetchClientContainer(query, filterParams, clientIds, pageable);

        List<ClientContainer> containers = clientContainerPage.getContent();
        
        Set<Long> requiredClientIds = containers.stream()
                .map(ClientContainer::getClient)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, ClientDTO> clientMap = fetchClientsByIds(requiredClientIds);

        List<ClientContainerResponseDTO> clientContainerResponseDTOs = containers.stream()
                .map(clientContainer -> mapToClientContainerPageDTO(clientContainer, clientMap))
                .toList();

        return new PageResponse<>(
                clientContainerPage.getNumber(),
                clientContainerPage.getSize(),
                clientContainerPage.getTotalElements(),
                clientContainerPage.getTotalPages(),
                clientContainerResponseDTOs);
    }

    private ClientData fetchClientData(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        try {
            if (hasClientFilters(clientFilterParams, query)) {
                ClientData clientData = fetchClientIdsWithFilters(query, clientFilterParams, clientTypeId);
                if (clientData.clientIds() != null && clientData.clientIds().isEmpty() && 
                    StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                    return new ClientData(null, Collections.emptyMap());
                }
                return clientData;
            } else {
                return new ClientData(null, Collections.emptyMap());
            }
        } catch (FeignException e) {
            log.error("Feign error fetching client data: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            if (StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                return new ClientData(null, Collections.emptyMap());
            }
            return new ClientData(Collections.emptyList(), Collections.emptyMap());
        } catch (Exception e) {
            log.error("Unexpected error fetching client data: query={}, error={}", query, e.getMessage(), e);
            if (StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                return new ClientData(null, Collections.emptyMap());
            }
            return new ClientData(Collections.emptyList(), Collections.emptyMap());
        }
    }

    private boolean hasClientFilters(@NonNull Map<String, List<String>> clientFilterParams, String query) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasClientFilters = !clientFilterParams.isEmpty();
        return hasClientFilters || hasQuery;
    }

    private ClientData fetchClientIdsWithFilters(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        Map<String, List<String>> filteredParams = clientFilterParams.entrySet().stream()
                .filter(entry -> isClientFilterKey(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> mapClientFilterKey(entry.getKey()),
                        Map.Entry::getValue));
        
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
        List<Long> clientIds = clientApiClient.searchClientIds(clientRequest).getBody();
        if (clientIds == null) {
            clientIds = Collections.emptyList();
        }
        return new ClientData(clientIds, Collections.emptyMap());
    }

    private Map<Long, ClientDTO> fetchClientsByIds(@NonNull Set<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            List<Long> clientIdsList = new ArrayList<>(clientIds);
            List<ClientDTO> clients = clientApiClient.getClientsByIds(clientIdsList).getBody();
            if (clients == null) {
                clients = Collections.emptyList();
            }
            return buildClientMap(clients);
        } catch (FeignException e) {
            log.error("Feign error fetching clients by IDs: status={}, error={}", e.status(), e.getMessage(), e);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Unexpected error fetching clients by IDs: error={}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    private Map<Long, ClientDTO> buildClientMap(@NonNull List<ClientDTO> clients) {
        return clients.stream()
                .filter(Objects::nonNull)
                .filter(client -> client.getId() != null)
                .collect(Collectors.toMap(
                        ClientDTO::getId,
                        client -> client,
                        (existing, _) -> existing
                ));
    }

    private boolean isClientFilterKey(String key) {
        return FILTER_KEY_SOURCE.equals(key) ||
                FILTER_KEY_CLIENT_SOURCE.equals(key) ||
                FILTER_KEY_CLIENT_CREATED_AT_FROM.equals(key) ||
                FILTER_KEY_CLIENT_CREATED_AT_TO.equals(key) ||
                FILTER_KEY_CLIENT_UPDATED_AT_FROM.equals(key) ||
                FILTER_KEY_CLIENT_UPDATED_AT_TO.equals(key) ||
                key.startsWith(FILTER_PREFIX_FIELD);
    }

    private String mapClientFilterKey(String key) {
        return switch (key) {
            case FILTER_KEY_CLIENT_SOURCE -> MAPPED_KEY_SOURCE;
            case FILTER_KEY_CLIENT_CREATED_AT_FROM -> MAPPED_KEY_CREATED_AT_FROM;
            case FILTER_KEY_CLIENT_CREATED_AT_TO -> MAPPED_KEY_CREATED_AT_TO;
            case FILTER_KEY_CLIENT_UPDATED_AT_FROM -> MAPPED_KEY_UPDATED_AT_FROM;
            case FILTER_KEY_CLIENT_UPDATED_AT_TO -> MAPPED_KEY_UPDATED_AT_TO;
            default -> key;
        };
    }

    private ClientContainerResponseDTO mapToClientContainerPageDTO(@NonNull ClientContainer clientContainer,
                                                                   @NonNull Map<Long, ClientDTO> clientMap) {
        ClientContainerResponseDTO dto = new ClientContainerResponseDTO();
        dto.setUserId(clientContainer.getUser());
        dto.setClient(clientMap.get(clientContainer.getClient()));
        dto.setContainerId(clientContainer.getContainer().getId());
        dto.setContainerName(clientContainer.getContainer().getName());
        dto.setQuantity(clientContainer.getQuantity());
        dto.setUpdatedAt(clientContainer.getUpdatedAt());
        return dto;
    }

    private Page<ClientContainer> fetchClientContainer(String query,
                                                       Map<String, List<String>> filterParams,
                                                       List<Long> clientIds,
                                                       @NonNull Pageable pageable) {
        if (clientIds != null && clientIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        Map<String, List<String>> containerFilterParams = filterParams != null
                ? filterParams.entrySet().stream()
                        .filter(entry -> isContainerFilterKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : Collections.emptyMap();

        Specification<ClientContainer> spec = new ClientContainerSpecification(query, containerFilterParams, clientIds);
        return clientContainerRepository.findAll(spec, pageable);
    }

    private boolean isContainerFilterKey(String key) {
        return !FILTER_KEY_CLIENT_TYPE_ID.equals(key) &&
                !FILTER_KEY_SOURCE.equals(key) &&
                !FILTER_KEY_CLIENT_SOURCE.equals(key) &&
                !FILTER_KEY_CLIENT_CREATED_AT_FROM.equals(key) &&
                !FILTER_KEY_CLIENT_CREATED_AT_TO.equals(key) &&
                !FILTER_KEY_CLIENT_UPDATED_AT_FROM.equals(key) &&
                !FILTER_KEY_CLIENT_UPDATED_AT_TO.equals(key) &&
                !key.startsWith(FILTER_PREFIX_FIELD);
    }
}