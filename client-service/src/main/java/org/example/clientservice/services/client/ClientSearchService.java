package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PhoneNumber;
import org.example.clientservice.models.field.*;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.repositories.PhoneNumberRepository;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.models.client.FilterIds;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSearchService implements IClientSearchService {
    private final ClientRepository clientRepository;
    private final IBusinessService businessService;
    private final IRegionService regionService;
    private final IRouteService routeService;
    private final ISourceService sourceService;
    private final IStatusClientService statusClientService;
    private final PhoneNumberRepository phoneNumberRepository;

    @Override
    public Page<Client> searchClients(String query, Pageable pageable, Map<String, List<String>> filterParams,
                                      String excludedStatuses) {
        validateQuery(query);
        validateFilterParams(filterParams);

        List<Long> excludeStatusIds = parseExcludedStatuses(excludedStatuses);

        if (query == null || query.trim().isEmpty()) {
            return fetchClients(null, filterParams, null, excludeStatusIds, pageable);
        }

        FilterIds filterIds = fetchFilterIds(query);
        return fetchClients(query, filterParams, filterIds, excludeStatusIds, pageable);
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
        Set<String> validKeys = Set.of("createdAtFrom", "createdAtTo", "updatedAtFrom", "updatedAtTo",
                "business", "route", "region", "status", "source");
        for (String key : filterParams.keySet()) {
            if (!validKeys.contains(key)) {
                throw new ClientException(String.format("Invalid filter key: %s", key));
            }
        }
    }

    private List<Long> parseExcludedStatuses(String excludedStatuses) {
        if (excludedStatuses == null || excludedStatuses.trim().isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(excludedStatuses.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            throw new ClientException(String.format("Incorrect status format for exclusion: %s", excludedStatuses));
        }
    }

    private FilterIds fetchFilterIds(String query) {
        List<Business> businessData = businessService.findByNameContaining(query);
        List<Region> regionData = regionService.findByNameContaining(query);
        List<Route> routeData = routeService.findByNameContaining(query);
        List<Source> sourceData = sourceService.findByNameContaining(query);
        List<StatusClient> statusClientData = statusClientService.findByNameContaining(query);

        List<Long> businessIds = extractIds(businessData, Business::getId);
        List<Long> regionIds = extractIds(regionData, Region::getId);
        List<Long> routeIds = extractIds(routeData, Route::getId);
        List<Long> sourceIds = extractIds(sourceData, Source::getId);
        List<Long> statusClientIds = extractIds(statusClientData, StatusClient::getId);

        return new FilterIds(
                businessData, businessIds,
                regionData, regionIds,
                routeData, routeIds,
                sourceData, sourceIds,
                statusClientData, statusClientIds
        );
    }

    private <T> List<Long> extractIds(List<T> dataList, Function<T, Long> idExtractor) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyList();
        }
        return dataList.stream()
                .map(idExtractor)
                .collect(Collectors.toList());
    }

    private Page<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      List<Long> excludeStatusIds, Pageable pageable) {
        Page<Client> clientPage = clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds != null ? filterIds.statusIds() : null,
                filterIds != null ? filterIds.sourceIds() : null,
                filterIds != null ? filterIds.routeIds() : null,
                filterIds != null ? filterIds.regionIds() : null,
                filterIds != null ? filterIds.businessIds() : null,
                excludeStatusIds
        ), pageable);

        if (!clientPage.getContent().isEmpty()) {
            List<Long> clientIds = clientPage.getContent().stream()
                    .map(Client::getId)
                    .collect(Collectors.toList());
            List<PhoneNumber> phoneNumbers = phoneNumberRepository.findByClientIdIn(clientIds);

            Map<Long, List<PhoneNumber>> phoneNumbersByClientId = phoneNumbers.stream()
                    .collect(Collectors.groupingBy(phoneNumber -> phoneNumber.getClient().getId()));

            clientPage.getContent().forEach(client -> {
                List<PhoneNumber> clientPhoneNumbers = phoneNumbersByClientId.getOrDefault(client.getId(), new ArrayList<>());
                client.setPhoneNumbers(clientPhoneNumbers);
            });
        }

        return clientPage;
    }

    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds) {
        List<Client> clients = clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds != null ? filterIds.statusIds() : null,
                filterIds != null ? filterIds.sourceIds() : null,
                filterIds != null ? filterIds.routeIds() : null,
                filterIds != null ? filterIds.regionIds() : null,
                filterIds != null ? filterIds.businessIds() : null,
                null
        ));

        if (!clients.isEmpty()) {
            List<Long> clientIds = clients.stream()
                    .map(Client::getId)
                    .collect(Collectors.toList());
            List<PhoneNumber> phoneNumbers = phoneNumberRepository.findByClientIdIn(clientIds);

            Map<Long, List<PhoneNumber>> phoneNumbersByClientId = phoneNumbers.stream()
                    .collect(Collectors.groupingBy(phoneNumber -> phoneNumber.getClient().getId()));

            clients.forEach(client -> {
                List<PhoneNumber> clientPhoneNumbers = phoneNumbersByClientId.getOrDefault(client.getId(), new ArrayList<>());
                client.setPhoneNumbers(clientPhoneNumbers);
            });
        }

        return clients;
    }
}
