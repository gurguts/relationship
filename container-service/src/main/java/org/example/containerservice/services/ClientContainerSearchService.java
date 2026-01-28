package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.ClientData;
import org.example.containerservice.models.dto.PageResponse;
import org.example.containerservice.models.dto.client.ClientDTO;
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

    private final ClientContainerRepository clientContainerRepository;
    private final ClientDataFetcher clientDataFetcher;
    private final ClientFilterMapper clientFilterMapper;
    private final ClientContainerMapper clientContainerMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientContainerResponseDTO> searchClientContainer(String query,
                                                                          @NonNull Pageable pageable,
                                                                          Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = FilterUtils.filterClientParams(filterParams);
        
        boolean hasClientFilters = clientFilterMapper.hasClientFilters(clientFilterParams, query);
        Map<String, List<String>> mappedClientFilterParams = clientFilterMapper.mapClientFilterParams(clientFilterParams);
        ClientData clientData = clientDataFetcher.fetchClientData(query, mappedClientFilterParams, clientTypeId, hasClientFilters);
        List<Long> clientIds = clientData.clientIds();

        Page<ClientContainer> clientContainerPage = fetchClientContainer(query, filterParams, clientIds, pageable);

        List<ClientContainer> containers = clientContainerPage.getContent();
        
        Set<Long> requiredClientIds = containers.stream()
                .map(ClientContainer::getClient)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, ClientDTO> clientMap = clientDataFetcher.fetchClientsByIds(requiredClientIds);

        List<ClientContainerResponseDTO> clientContainerResponseDTOs = containers.stream()
                .map(clientContainer -> clientContainerMapper.mapToClientContainerPageDTO(clientContainer, clientMap))
                .toList();

        return new PageResponse<>(
                clientContainerPage.getNumber(),
                clientContainerPage.getSize(),
                clientContainerPage.getTotalElements(),
                clientContainerPage.getTotalPages(),
                clientContainerResponseDTOs);
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
                        .filter(entry -> clientFilterMapper.isContainerFilterKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : Collections.emptyMap();

        Specification<ClientContainer> spec = new ClientContainerSpecification(query, containerFilterParams, clientIds);
        return clientContainerRepository.findAll(spec, pageable);
    }
}