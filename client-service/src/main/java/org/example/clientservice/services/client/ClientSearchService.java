package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.field.*;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.models.client.FilterIds;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSearchService implements IClientSearchService {
    private final ClientRepository clientRepository;
    private final ISourceService sourceService;

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
        Set<String> validKeys = Set.of("createdAtFrom", "createdAtTo", "updatedAtFrom", "updatedAtTo", "source");
        for (String key : filterParams.keySet()) {
            if (!validKeys.contains(key) && !key.endsWith("From") && !key.endsWith("To")) {
                log.warn("Unknown filter key: {}, skipping", key);
            }
        }
    }

    private FilterIds fetchFilterIds(String query) {
        List<Source> sourceData = sourceService.findByNameContaining(query);
        List<Long> sourceIds = sourceData.stream().map(Source::getId).collect(Collectors.toList());

        return new FilterIds(sourceData, sourceIds);
    }

    private Page<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      Pageable pageable, Long clientTypeId) {
        Page<Client> clientPage = clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds != null ? filterIds.sourceIds() : null,
                clientTypeId
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
}
