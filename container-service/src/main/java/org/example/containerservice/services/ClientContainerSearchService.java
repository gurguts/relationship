package org.example.containerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.clients.ClientApiClient;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.ClientData;
import org.example.containerservice.models.dto.container.ClientContainerResponseDTO;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.example.containerservice.models.dto.PageResponse;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.spec.ClientContainerSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerSearchService {

    private final ClientContainerRepository clientContainerRepository;
    private final ClientApiClient clientApiClient;

    public PageResponse<ClientContainerResponseDTO> searchClientContainer(String query, Pageable pageable,
                                                                          Map<String, List<String>> filterParams) {
        ClientData clientData = fetchClientData(query, filterParams);
        List<Long> clientIds = clientData.clientIds();
        Map<Long, ClientDTO> clientMap = clientData.clientMap();

        Page<ClientContainer> clientContainerPage = fetchClientContainer(query, filterParams, clientIds, pageable);

        List<ClientContainerResponseDTO> clientContainerResponseDTOS = clientContainerPage.getContent().stream()
                .map(clientContainer -> mapToClientContainerPageDTO(clientContainer, clientMap))
                .collect(Collectors.toList());

        return new PageResponse<>(clientContainerPage.getNumber(), clientContainerPage.getSize(),
                clientContainerPage.getTotalElements(),
                clientContainerPage.getTotalPages(), clientContainerResponseDTOS);
    }

    private ClientData fetchClientData(String query, Map<String, List<String>> filterParams) {
        Map<String, List<String>> filteredParams = filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("status") || key.equals("business") ||
                            key.equals("route") || key.equals("region") || key.equals("source");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams);
        List<ClientDTO> clients = clientApiClient.searchClients(clientRequest);
        List<Long> clientIds = clients.stream()
                .map(ClientDTO::getId)
                .collect(Collectors.toList());
        Map<Long, ClientDTO> clientMap = clients.stream()
                .collect(Collectors.toMap(ClientDTO::getId, client -> client));
        return new ClientData(clientIds, clientMap);
    }

    private ClientContainerResponseDTO mapToClientContainerPageDTO(ClientContainer clientContainer,
                                                                   Map<Long, ClientDTO> clientMap) {
        ClientContainerResponseDTO dto = new ClientContainerResponseDTO();
        dto.setUserId(clientContainer.getUser());
        dto.setClient(clientMap.get(clientContainer.getClient()));
        dto.setContainerId(clientContainer.getContainer().getId());
        dto.setContainerName(clientContainer.getContainer().getName());
        dto.setQuantity(clientContainer.getQuantity());
        dto.setUpdatedAt(clientContainer.getUpdatedAt());
        return dto;
    }

    private Page<ClientContainer> fetchClientContainer(String query, Map<String, List<String>> filterParams,
                                                       List<Long> clientIds, Pageable pageable) {
        Specification<ClientContainer> spec = new ClientContainerSpecification(query, filterParams, clientIds);
        return clientContainerRepository.findAll(spec, pageable);
    }
}