package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.models.ClientData;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.services.impl.ISourceService;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import feign.FeignException;

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
public class PurchaseClientDataFetcher {
    
    private final ClientApiClient clientApiClient;
    private final ISourceService sourceService;
    
    public ClientData fetchClientData(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        try {
            if (hasClientFilters(clientFilterParams, query) || clientTypeId != null) {
                List<Long> clientIds = fetchClientIdsWithFilters(query, clientFilterParams, clientTypeId);
                if (clientTypeId != null) {
                    return new ClientData(clientIds, Collections.emptyMap());
                }
                if (clientIds.isEmpty() && StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                    return new ClientData(null, Collections.emptyMap());
                }
                return new ClientData(clientIds, Collections.emptyMap());
            } else {
                return new ClientData(null, Collections.emptyMap());
            }
        } catch (FeignException e) {
            log.error("Feign error fetching client data: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            return handleErrorResponse(query, clientFilterParams, clientTypeId);
        } catch (Exception e) {
            log.error("Unexpected error fetching client data: query={}, error={}", query, e.getMessage(), e);
            return handleErrorResponse(query, clientFilterParams, clientTypeId);
        }
    }
    
    public List<Long> resolveSourceIds(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            return fetchSourceIds(query);
        }
        return null;
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
    
    private boolean hasClientFilters(@NonNull Map<String, List<String>> clientFilterParams, String query) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasClientFilters = !clientFilterParams.isEmpty();
        return hasClientFilters || hasQuery;
    }

    private List<Long> fetchClientIdsWithFilters(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, clientFilterParams, clientTypeId);
        List<Long> clientIds = clientApiClient.searchClientIds(clientRequest).getBody();
        if (clientIds == null) {
            clientIds = Collections.emptyList();
        }
        return clientIds;
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

    private List<Long> fetchSourceIds(@NonNull String query) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SourceDTO> sources = sourceService.findByNameContaining(query);
        return sources.stream()
                .map(SourceDTO::getId)
                .filter(Objects::nonNull)
                .toList();
    }
    
    private ClientData handleErrorResponse(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        if (clientTypeId != null) {
            return new ClientData(Collections.emptyList(), Collections.emptyMap());
        }
        if (StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
            return new ClientData(null, Collections.emptyMap());
        }
        return new ClientData(Collections.emptyList(), Collections.emptyMap());
    }
}
