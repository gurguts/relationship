package org.example.clientservice.services.impl;

import org.example.clientservice.models.client.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IClientSearchService {
    Page<Client> searchClients(String query, Pageable pageable, Map<String, List<String>> filterParams, Long clientTypeId);

    List<Client> searchClientsForPurchase(String query, Map<String, List<String>> filterParams);

    List<Map<Long, String>> searchIdsClient(List<Long> ids);
}