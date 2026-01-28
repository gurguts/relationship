package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.ClientMapper;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.ClientFilterIds;
import org.example.clientservice.models.client.PageResponse;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.dto.client.ClientDTO;
import org.example.clientservice.models.dto.client.ClientListDTO;
import org.example.clientservice.models.dto.client.ClientSearchRequest;
import org.example.clientservice.models.dto.client.ExternalClientDataCache;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.repositories.clienttype.ClientFieldValueRepository;
import org.example.clientservice.services.impl.IClientSearchService;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.spec.ClientSpecification;
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
    
    private final ClientRepository clientRepository;
    private final ClientFieldValueRepository fieldValueRepository;
    private final ISourceService sourceService;
    private final ClientMapper clientMapper;
    private final ClientSearchValidator validator;
    private final ClientSearchQueryProcessor queryProcessor;
    private final ClientSearchPermissionResolver permissionResolver;

    @Override
    @NonNull
    public PageResponse<ClientDTO> searchClients(String query, int size, int page, String sortProperty,
                                                Sort.Direction sortDirection, String filtersJson, Long clientTypeId) {
        validator.validatePaginationParams(size, page);
        
        String normalizedQuery = queryProcessor.normalizeQuery(query);
        validator.validateQuery(normalizedQuery);
        
        Map<String, List<String>> filters = queryProcessor.parseFilters(filtersJson);
        String validatedSortProperty = validator.validateAndNormalizeSortProperty(sortProperty);
        Sort.Direction validatedSortDirection = validator.validateSortDirection(sortProperty, sortDirection);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(validatedSortDirection, validatedSortProperty));
        
        Page<Client> clientPage = searchClientsInternal(normalizedQuery, pageable, filters, clientTypeId);
        Page<ClientDTO> clientDTOPage = clientPage.map(clientMapper::clientToClientDTO);
        
        return new PageResponse<>(clientDTOPage);
    }

    @Override
    @NonNull
    public List<ClientListDTO> searchClientsForPurchase(@NonNull ClientSearchRequest request) {
        SearchParams searchParams = prepareSearchParams(request);
        List<Client> clients = fetchClients(searchParams.normalizedQuery(), searchParams.cleanedFilterParams(), 
                searchParams.filterIds(), request.clientTypeId());
        
        ExternalClientDataCache cache = ExternalClientDataCache.of(sourceService.getAllSources());
        
        return clients.stream()
                .map(client -> clientMapper.clientToClientListDTO(client, cache))
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<Map<Long, String>> searchIdsClient(@NonNull List<Long> ids) {
        validator.validateIds(ids);
        
        List<Client> clients = clientRepository.findAllById(ids);
        return clients.stream()
                .filter(client -> client.getId() != null)
                .map(client -> Map.of(client.getId(), client.getCompany()))
                .toList();
    }
    
    
    private Page<Client> searchClientsInternal(String query, @NonNull Pageable pageable, 
                                               @NonNull Map<String, List<String>> filterParams,
                                               Long clientTypeId) {
        ClientFilterIds filterIds = query != null ? queryProcessor.fetchFilterIds(query) : null;
        
        return fetchClientsWithPagination(query, filterParams, filterIds, clientTypeId, pageable);
    }

    private Page<Client> fetchClientsWithPagination(String query, @NonNull Map<String, List<String>> filterParams, 
                                                    ClientFilterIds filterIds, Long clientTypeId, 
                                                    @NonNull Pageable pageable) {
        List<Long> allowedClientTypeIds = permissionResolver.determineAllowedClientTypeIds(clientTypeId);
        
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

    private SearchParams prepareSearchParams(@NonNull ClientSearchRequest request) {
        String normalizedQuery = queryProcessor.normalizeQuery(request.query());
        validator.validateQuery(normalizedQuery);
        
        ClientFilterIds filterIds = normalizedQuery != null 
                ? queryProcessor.fetchFilterIds(normalizedQuery) 
                : null;
        
        Map<String, List<String>> cleanedFilterParams = queryProcessor.cleanFilterParamsForPurchase(request.filterParams());
        
        return new SearchParams(normalizedQuery, cleanedFilterParams, filterIds);
    }

    private record SearchParams(
            String normalizedQuery,
            Map<String, List<String>> cleanedFilterParams,
            ClientFilterIds filterIds
    ) {
    }

    @Override
    @NonNull
    public List<Long> searchClientIds(@NonNull ClientSearchRequest request) {
        SearchParams searchParams = prepareSearchParams(request);
        List<Client> clients = fetchClients(searchParams.normalizedQuery(), searchParams.cleanedFilterParams(), 
                searchParams.filterIds(), request.clientTypeId());
        
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
        
        validator.validateClientIds(clientIds);
        
        List<Client> clients = clientRepository.findAllById(clientIds);
        return clients.stream()
                .map(clientMapper::clientToClientDTO)
                .collect(Collectors.toList());
    }
}
