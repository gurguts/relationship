package org.example.containerservice.services;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.clients.ClientApiClient;
import org.example.containerservice.models.ClientData;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.springframework.stereotype.Service;
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
public class ClientDataFetcher {

    private final ClientApiClient clientApiClient;

    public ClientData fetchClientData(String query, 
                                     @NonNull Map<String, List<String>> clientFilterParams, 
                                     Long clientTypeId,
                                     boolean hasClientFilters) {
        try {
            if (hasClientFilters || clientTypeId != null) {
                ClientData clientData = fetchClientIdsWithFilters(query, clientFilterParams, clientTypeId);
                return processClientData(clientData, query, clientFilterParams, clientTypeId);
            } else {
                return new ClientData(null, Collections.emptyMap());
            }
        } catch (FeignException e) {
            log.error("Feign error fetching client data: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            return handleError(query, clientFilterParams, clientTypeId);
        } catch (Exception e) {
            log.error("Unexpected error fetching client data: query={}, error={}", query, e.getMessage(), e);
            return handleError(query, clientFilterParams, clientTypeId);
        }
    }

    public Map<Long, ClientDTO> fetchClientsByIds(@NonNull Set<Long> clientIds) {
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

    private ClientData fetchClientIdsWithFilters(String query, 
                                                 @NonNull Map<String, List<String>> clientFilterParams, 
                                                 Long clientTypeId) {
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, clientFilterParams, clientTypeId);
        List<Long> clientIds = clientApiClient.searchClientIds(clientRequest).getBody();
        if (clientIds == null) {
            clientIds = Collections.emptyList();
        }
        return new ClientData(clientIds, Collections.emptyMap());
    }

    private ClientData processClientData(ClientData clientData, 
                                       String query, 
                                       @NonNull Map<String, List<String>> clientFilterParams, 
                                       Long clientTypeId) {
        if (clientTypeId != null) {
            if (clientData.clientIds() == null) {
                return new ClientData(Collections.emptyList(), Collections.emptyMap());
            }
            return clientData;
        }
        if (clientData.clientIds() != null && clientData.clientIds().isEmpty() && 
            StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
            return new ClientData(null, Collections.emptyMap());
        }
        return clientData;
    }

    private ClientData handleError(String query, 
                                  @NonNull Map<String, List<String>> clientFilterParams, 
                                  Long clientTypeId) {
        if (clientTypeId != null) {
            return new ClientData(Collections.emptyList(), Collections.emptyMap());
        }
        if (StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
            return new ClientData(null, Collections.emptyMap());
        }
        return new ClientData(Collections.emptyList(), Collections.emptyMap());
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
}
