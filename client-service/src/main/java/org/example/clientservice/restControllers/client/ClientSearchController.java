package org.example.clientservice.restControllers.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.mappers.ClientMapper;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PageResponse;
import org.example.clientservice.models.dto.client.ClientDTO;
import org.example.clientservice.models.dto.client.ClientListDTO;
import org.example.clientservice.models.dto.client.ClientSearchRequest;
import org.example.clientservice.models.dto.client.ExternalClientDataCache;
import org.example.clientservice.services.impl.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Slf4j
public class ClientSearchController {
    private final IClientSearchService clientService;
    private final ClientMapper clientMapper;
    private final ObjectMapper objectMapper;
    private final IRouteService routeService;
    private final IRegionService regionService;
    private final IStatusClientService statusClientService;
    private final ISourceService sourceService;
    private final IBusinessService businessService;
    private final IClientProductService clientProductService;

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ClientDTO>> searchClients(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "size", defaultValue = "100") @Positive @Max(1000) int size,
            @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson,
            @RequestParam(name = "excludeStatuses", required = false) String excludeStatuses
    ) {

        if (query != null) {
            query = query.trim();
        }

        Map<String, List<String>> filters = parseFilters(filtersJson);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Page<ClientDTO> clients = clientService.searchClients(query, pageable, filters, excludeStatuses)
                .map(clientMapper::clientToClientDTO);

        PageResponse<ClientDTO> response = new PageResponse<>(clients);
        return ResponseEntity.ok(response);
    }

    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ClientException("Invalid JSON format for filters");
        }
    }

    @PostMapping("/search")
    public List<ClientListDTO> searchClientsForPurchase(@RequestBody ClientSearchRequest request) {
        List<Client> clients = clientService.searchClientsForPurchase(request.query(), request.filterParams());

        ExternalClientDataCache cache = new ExternalClientDataCache(
                businessService.getAllBusinesses(),
                routeService.getAllRoutes(),
                regionService.getAllRegions(),
                statusClientService.getAllStatusClients(),
                sourceService.getAllSources(),
                clientProductService.getAllClientProducts()
        );

        return clients.stream()
                .map(client -> clientMapper.clientToClientListDTO(client, cache))
                .collect(Collectors.toList());
    }

    @PostMapping("/ids")
    public ResponseEntity<List<Map<Long, String>>> getIdsForClient(@RequestBody List<Long> ids) {
        List<Map<Long, String>> clients = clientService.searchIdsClient(ids);
        return ResponseEntity.ok(clients);
    }
}
