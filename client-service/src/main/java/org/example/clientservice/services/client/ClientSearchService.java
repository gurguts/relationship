package org.example.clientservice.services.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.mappers.ClientMapper;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PageResponse;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.client.ClientDTO;
import org.example.clientservice.models.dto.client.ClientListDTO;
import org.example.clientservice.models.dto.client.ClientSearchRequest;
import org.example.clientservice.models.dto.client.ExternalClientDataCache;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.repositories.clienttype.ClientFieldValueRepository;
import org.example.clientservice.services.impl.IClientSearchService;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.models.client.ClientFilterIds;
import org.example.clientservice.spec.ClientSpecification;
import org.example.clientservice.utils.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSearchService implements IClientSearchService {
    
    private static final Set<String> VALID_SORT_PROPERTIES = Set.of("company", "source", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_PROPERTY = "updatedAt";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
    private static final TypeReference<Map<String, List<String>>> FILTERS_TYPE_REFERENCE =
            new TypeReference<>() {
            };
    private static final int MAX_QUERY_LENGTH = 255;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int MAX_IDS_LIMIT = 1000;
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    
    private final ClientRepository clientRepository;
    private final ClientFieldValueRepository fieldValueRepository;
    private final ISourceService sourceService;
    private final IClientTypePermissionService clientTypePermissionService;
    private final ClientMapper clientMapper;
    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public PageResponse<ClientDTO> searchClients(String query, int size, int page, String sortProperty,
                                                Sort.Direction sortDirection, String filtersJson, Long clientTypeId) {
        validatePaginationParams(size, page);
        
        String normalizedQuery = normalizeQuery(query);
        validateQuery(normalizedQuery);
        
        Map<String, List<String>> filters = parseFilters(filtersJson);
        String validatedSortProperty = validateAndNormalizeSortProperty(sortProperty);
        Sort.Direction validatedSortDirection = validateSortDirection(sortProperty, sortDirection);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(validatedSortDirection, validatedSortProperty));
        
        Page<Client> clientPage = searchClientsInternal(normalizedQuery, pageable, filters, clientTypeId);
        Page<ClientDTO> clientDTOPage = clientPage.map(clientMapper::clientToClientDTO);
        
        return new PageResponse<>(clientDTOPage);
    }

    @Override
    @NonNull
    public List<ClientListDTO> searchClientsForPurchase(@NonNull ClientSearchRequest request) {
        String normalizedQuery = normalizeQuery(request.query());
        validateQuery(normalizedQuery);
        
        ClientFilterIds filterIds = normalizedQuery != null 
                ? fetchFilterIds(normalizedQuery) 
                : null;
        
        Map<String, List<String>> cleanedFilterParams = cleanFilterParamsForPurchase(request.filterParams());
        List<Client> clients = fetchClients(normalizedQuery, cleanedFilterParams, filterIds, request.clientTypeId());
        
        ExternalClientDataCache cache = ExternalClientDataCache.of(sourceService.getAllSources());
        
        return clients.stream()
                .map(client -> clientMapper.clientToClientListDTO(client, cache))
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<Map<Long, String>> searchIdsClient(@NonNull List<Long> ids) {
        validateIds(ids);
        
        List<Client> clients = clientRepository.findAllById(ids);
        return clients.stream()
                .filter(client -> client.getId() != null)
                .map(client -> Map.of(client.getId(), client.getCompany()))
                .toList();
    }
    
    private void validatePaginationParams(int size, int page) {
        if (size < MIN_PAGE_SIZE) {
            throw new ClientException("INVALID_PAGE_SIZE", 
                    String.format("Page size must be at least %d", MIN_PAGE_SIZE));
        }
        if (size > MAX_PAGE_SIZE) {
            throw new ClientException("INVALID_PAGE_SIZE", 
                    String.format("Page size cannot exceed %d", MAX_PAGE_SIZE));
        }
        if (page < 0) {
            throw new ClientException("INVALID_PAGE_NUMBER", "Page number must be non-negative");
        }
    }
    
    private void validateIds(@NonNull List<Long> ids) {
        if (ids.isEmpty()) {
            throw new ClientException("INVALID_IDS", "List of IDs cannot be empty");
        }
        if (ids.size() > MAX_IDS_LIMIT) {
            throw new ClientException("INVALID_IDS", 
                    String.format("Cannot search for more than %d IDs at once", MAX_IDS_LIMIT));
        }
        if (ids.contains(null)) {
            throw new ClientException("INVALID_IDS", "List of IDs cannot contain null values");
        }
    }
    
    private Page<Client> searchClientsInternal(String query, @NonNull Pageable pageable, 
                                               @NonNull Map<String, List<String>> filterParams,
                                               Long clientTypeId) {
        ClientFilterIds filterIds = query != null ? fetchFilterIds(query) : null;
        
        return fetchClientsWithPagination(query, filterParams, filterIds, clientTypeId, pageable);
    }
    
    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(filtersJson, FILTERS_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse filters JSON: {}", e.getMessage(), e);
            throw new ClientException("INVALID_JSON", "Invalid JSON format for filters");
        }
    }
    
    private String validateAndNormalizeSortProperty(String sortProperty) {
        if (sortProperty == null || !VALID_SORT_PROPERTIES.contains(sortProperty)) {
            return DEFAULT_SORT_PROPERTY;
        }
        return sortProperty;
    }
    
    private Sort.Direction validateSortDirection(String sortProperty, Sort.Direction sortDirection) {
        if (sortProperty == null || !VALID_SORT_PROPERTIES.contains(sortProperty)) {
            return DEFAULT_SORT_DIRECTION;
        }
        return sortDirection != null ? sortDirection : DEFAULT_SORT_DIRECTION;
    }

    private void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ClientException("INVALID_QUERY", 
                    String.format("Search query cannot exceed %d characters", MAX_QUERY_LENGTH));
        }
    }

    private ClientFilterIds fetchFilterIds(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ClientFilterIds(Collections.emptyList(), Collections.emptyList());
        }
        
        List<Source> sourceData = sourceService.findByNameContaining(query);
        List<Long> sourceIds = sourceData.stream()
                .map(Source::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ClientFilterIds(sourceData, sourceIds);
    }

    private Page<Client> fetchClientsWithPagination(String query, @NonNull Map<String, List<String>> filterParams, 
                                                    ClientFilterIds filterIds, Long clientTypeId, 
                                                    @NonNull Pageable pageable) {
        List<Long> allowedClientTypeIds = determineAllowedClientTypeIds(clientTypeId);
        
        if (allowedClientTypeIds != null && allowedClientTypeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        ClientSpecification specification = createClientSpecification(query, filterParams, filterIds, 
                clientTypeId, allowedClientTypeIds);
        
        Page<Client> clientPage = clientRepository.findAll(specification, pageable);
        
        loadFieldValuesIfNeeded(clientPage.getContent());
        
        return clientPage;
    }

    private List<Client> fetchClients(String query, @NonNull Map<String, List<String>> filterParams,
                                     ClientFilterIds filterIds, Long clientTypeId) {
        ClientSpecification specification = createClientSpecification(query, filterParams, filterIds, 
                clientTypeId, null);

        return clientRepository.findAll(specification);
    }
    
    private void loadFieldValuesIfNeeded(@NonNull List<Client> clients) {
        if (!clients.isEmpty()) {
            loadFieldValuesForClients(clients);
        }
    }
    
    private List<Long> determineAllowedClientTypeIds(Long clientTypeId) {
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
    
    private ClientSpecification createClientSpecification(String query, @NonNull Map<String, List<String>> filterParams,
                                                          ClientFilterIds filterIds, Long clientTypeId,
                                                          List<Long> allowedClientTypeIds) {
        List<Long> sourceIds = filterIds != null ? filterIds.sourceIds() : null;
        return new ClientSpecification(query, filterParams, sourceIds, clientTypeId, allowedClientTypeIds);
    }
    
    private void loadFieldValuesForClients(@NonNull List<Client> clients) {
        List<Long> clientIds = clients.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (clientIds.isEmpty()) {
            return;
        }
        
        List<ClientFieldValue> fieldValues = fieldValueRepository.findByClientIdInWithFieldAndValueList(clientIds);
        
        Map<Long, List<ClientFieldValue>> fieldValuesMap = fieldValues.stream()
                .filter(fv -> fv.getClient().getId() != null)
                .collect(Collectors.groupingBy(fv -> fv.getClient().getId()));
        
        clients.forEach(client -> {
            List<ClientFieldValue> clientFieldValues = fieldValuesMap.getOrDefault(
                    client.getId(), Collections.emptyList());
            client.setFieldValues(clientFieldValues);
        });
    }
    
    private Map<String, List<String>> cleanFilterParamsForPurchase(Map<String, List<String>> filterParams) {
        if (filterParams == null || filterParams.isEmpty()) {
            return Collections.emptyMap();
        }
        
        if (!filterParams.containsKey(FILTER_KEY_CLIENT_TYPE_ID)) {
            return Collections.unmodifiableMap(filterParams);
        }
        
        Map<String, List<String>> cleaned = new HashMap<>(filterParams);
        cleaned.remove(FILTER_KEY_CLIENT_TYPE_ID);
        return cleaned;
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

    @Override
    @NonNull
    public List<Long> searchClientIds(@NonNull ClientSearchRequest request) {
        String normalizedQuery = normalizeQuery(request.query());
        validateQuery(normalizedQuery);
        
        ClientFilterIds filterIds = normalizedQuery != null 
                ? fetchFilterIds(normalizedQuery) 
                : null;
        
        Map<String, List<String>> cleanedFilterParams = cleanFilterParamsForPurchase(request.filterParams());
        List<Client> clients = fetchClients(normalizedQuery, cleanedFilterParams, filterIds, request.clientTypeId());
        
        return clients.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<ClientDTO> getClientsByIds(@NonNull List<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        if (clientIds.size() > MAX_IDS_LIMIT) {
            throw new ClientException("TOO_MANY_IDS", 
                    String.format("Cannot request more than %d client IDs at once", MAX_IDS_LIMIT));
        }
        
        List<Client> clients = clientRepository.findAllById(clientIds);
        return clients.stream()
                .map(clientMapper::clientToClientDTO)
                .collect(Collectors.toList());
    }
}
