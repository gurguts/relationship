package org.example.clientservice.restControllers.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.client.PageResponse;
import org.example.clientservice.models.dto.client.ClientDTO;
import org.example.clientservice.models.dto.client.ClientListDTO;
import org.example.clientservice.models.dto.client.ClientSearchRequest;
import org.example.clientservice.services.impl.IClientSearchService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Validated
public class ClientSearchController {
    private final IClientSearchService clientService;

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ClientDTO>> searchClients(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "size", defaultValue = "100") @Positive @Max(1000) int size,
            @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson,
            @RequestParam(name = "clientTypeId", required = false) @Positive Long clientTypeId) {
        PageResponse<ClientDTO> response = clientService.searchClients(
                query, size, page, sortProperty, sortDirection, filtersJson, clientTypeId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/search")
    public ResponseEntity<List<ClientListDTO>> searchClientsForPurchase(
            @RequestBody @Valid ClientSearchRequest request) {
        List<ClientListDTO> result = clientService.searchClientsForPurchase(request);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/ids")
    public ResponseEntity<List<Map<Long, String>>> getIdsForClient(
            @RequestBody JsonNode requestBody) {
        List<Long> clientIds = new ArrayList<>();
        
        if (requestBody.isArray()) {
            for (JsonNode node : requestBody) {
                if (node.isNumber()) {
                    clientIds.add(node.asLong());
                }
            }
        } else if (requestBody.has("clientIds") && requestBody.get("clientIds").isArray()) {
            JsonNode clientIdsNode = requestBody.get("clientIds");
            for (JsonNode node : clientIdsNode) {
                if (node.isNumber()) {
                    clientIds.add(node.asLong());
                }
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        if (clientIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Map<Long, String>> result = clientService.searchIdsClient(clientIds);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/ids/search")
    public ResponseEntity<List<Long>> searchClientIds(
            @RequestBody @Valid ClientSearchRequest request) {
        List<Long> result = clientService.searchClientIds(request);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/by-ids")
    public ResponseEntity<List<ClientDTO>> getClientsByIds(
            @RequestBody @Valid List<Long> clientIds) {
        List<ClientDTO> result = clientService.getClientsByIds(clientIds);
        return ResponseEntity.ok(result);
    }
}
