package org.example.containerservice.services;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.clients.ClientApiClient;
import org.example.containerservice.clients.ClientTypeFieldApiClient;
import org.example.containerservice.clients.UserApiClient;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.UserDTO;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.containerservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.containerservice.models.dto.fields.SourceDTO;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.spec.ClientContainerSpecification;
import org.example.containerservice.utils.FilterUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerExportDataFetcher {

    private final ClientContainerRepository clientContainerRepository;
    private final ClientApiClient clientApiClient;
    private final ClientTypeFieldApiClient clientTypeFieldApiClient;
    private final UserApiClient userClient;
    private final ClientFilterMapper clientFilterMapper;

    public record FilterIds(
            List<UserDTO> userDTOs, List<Long> userIds, List<SourceDTO> sourceDTOs
    ) {}

    public FilterIds fetchFilterIds() {
        List<UserDTO> userDTOs = userClient.getAllUsers().getBody();
        if (userDTOs == null) {
            userDTOs = Collections.emptyList();
        }
        List<Long> userIds = userDTOs.stream().map(UserDTO::getId).toList();

        return new FilterIds(userDTOs, userIds, Collections.emptyList());
    }

    public List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);

        Map<String, List<String>> filteredParams = filterParams != null
                ? clientFilterMapper.mapClientFilterParams(filterParams)
                : Collections.emptyMap();

        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
        List<ClientDTO> clients = clientApiClient.searchClients(clientRequest).getBody();
        return clients != null ? clients : Collections.emptyList();
    }

    public Map<Long, ClientDTO> fetchClientMap(@NonNull List<ClientDTO> clients) {
        return clients.stream()
                .filter(client -> client.getId() != null)
                .collect(Collectors.toMap(ClientDTO::getId, client -> client));
    }

    public List<ClientContainer> fetchClientContainers(String query,
                                                         Map<String, List<String>> filterParams,
                                                         @NonNull List<Long> clientIds,
                                                         @NonNull Sort sort) {
        Specification<ClientContainer> spec = (root, querySpec, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (clientIds.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            predicates.add(root.get("client").in(clientIds));

            Specification<ClientContainer> clientContainerSpec =
                    new ClientContainerSpecification(query, filterParams, clientIds);
            predicates.add(clientContainerSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return clientContainerRepository.findAll(spec, sort);
    }

    public Map<Long, List<ClientFieldValueDTO>> fetchClientFieldValues(@NonNull List<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<ClientFieldValueDTO>> result = new HashMap<>();
        int batchSize = 100;

        for (int i = 0; i < clientIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, clientIds.size());
            List<Long> batch = clientIds.subList(i, endIndex);

            try {
                org.example.containerservice.models.dto.client.ClientIdsRequest request =
                        new org.example.containerservice.models.dto.client.ClientIdsRequest(batch);
                Map<Long, List<ClientFieldValueDTO>> batchResult = clientApiClient.getClientFieldValuesBatch(request).getBody();
                if (batchResult != null) {
                    result.putAll(batchResult);
                }
            } catch (FeignException e) {
                log.error("Feign error fetching field values batch for clients: status={}, error={}",
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.warn("Failed to fetch field values batch for clients: {}", e.getMessage());
            }
        }

        return result;
    }

    public List<SourceDTO> fetchClientSourceDTOs(@NonNull List<ClientDTO> clients) {
        Map<Long, SourceDTO> uniqueSources = new HashMap<>();

        for (ClientDTO client : clients) {
            try {
                java.lang.reflect.Method getSourceMethod = client.getClass().getMethod("getSource");
                Object sourceObj = getSourceMethod.invoke(client);
                if (sourceObj != null) {
                    SourceDTO sourceDTO = (SourceDTO) sourceObj;
                    if (sourceDTO.getId() != null) {
                        uniqueSources.put(sourceDTO.getId(), sourceDTO);
                    }
                }
            } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException | ClassCastException _) {
            }
        }

        return new ArrayList<>(uniqueSources.values());
    }

    public Map<Long, ClientTypeFieldDTO> loadFieldMap(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ClientTypeFieldDTO> result = new HashMap<>();
        int batchSize = 100;

        for (int i = 0; i < fieldIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, fieldIds.size());
            List<Long> batch = fieldIds.subList(i, endIndex);

            try {
                org.example.containerservice.models.dto.clienttype.FieldIdsRequest request =
                        new org.example.containerservice.models.dto.clienttype.FieldIdsRequest(batch);
                List<ClientTypeFieldDTO> fields = clientTypeFieldApiClient.getFieldsByIds(request).getBody();
                if (fields != null) {
                    fields.stream()
                            .filter(Objects::nonNull)
                            .forEach(field -> result.putIfAbsent(field.getId(), field));
                }
            } catch (FeignException e) {
                log.error("Feign error fetching client type fields: status={}, error={}",
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error fetching client type fields: error={}", e.getMessage(), e);
            }
        }

        return result;
    }
}
