package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.client.PageResponse;
import org.example.clientservice.models.dto.client.ClientDTO;
import org.example.clientservice.models.dto.client.ClientListDTO;
import org.example.clientservice.models.dto.client.ClientSearchRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface IClientSearchService {
    PageResponse<ClientDTO> searchClients(String query, int size, int page, String sortProperty,
                                          Sort.Direction sortDirection, String filtersJson, Long clientTypeId);

    List<ClientListDTO> searchClientsForPurchase(@NonNull ClientSearchRequest request);

    List<Map<Long, String>> searchIdsClient(@NonNull List<Long> ids);
    
    List<Long> searchClientIds(@NonNull ClientSearchRequest request);
    
    List<ClientDTO> getClientsByIds(@NonNull List<Long> clientIds);
}