package org.example.saleservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saleservice.clients.ClientApiClient;
import org.example.saleservice.clients.SourceClient;
import org.example.saleservice.models.ClientData;
import org.example.saleservice.models.PageResponse;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.client.ClientDTO;
import org.example.saleservice.models.dto.client.ClientSearchRequest;
import org.example.saleservice.models.dto.fields.SalePageDTO;
import org.example.saleservice.models.dto.fields.SourceDTO;
import org.example.saleservice.repositories.SaleRepository;
import org.example.saleservice.services.impl.ISaleSearchService;
import org.example.saleservice.spec.SaleSpecification;
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
public class SaleSearchService implements ISaleSearchService {
    private final SaleRepository saleRepository;
    private final ClientApiClient clientApiClient;
    private final SourceClient sourceClient;

    @Override
    public PageResponse<SalePageDTO> searchSale(String query, Pageable pageable,
                                                Map<String, List<String>> filterParams) {
        ClientData clientData = fetchClientData(query, filterParams);
        List<Long> clientIds = clientData.clientIds();
        Map<Long, ClientDTO> clientMap = clientData.clientMap();

        List<Long> sourceIds = (query == null || query.trim().isEmpty()) ? null : fetchSourceIds(query);

        Page<Sale> purchasePage = fetchSales(query, filterParams, clientIds, sourceIds, pageable);

        List<SalePageDTO> purchaseDTOs = purchasePage.getContent().stream()
                .map(purchase -> mapToSalePageDTO(purchase, clientMap))
                .collect(Collectors.toList());

        return new PageResponse<>(purchasePage.getNumber(), purchasePage.getSize(), purchasePage.getTotalElements(),
                purchasePage.getTotalPages(), purchaseDTOs);
    }

    private ClientData fetchClientData(String query, Map<String, List<String>> filterParams) {
        Map<String, List<String>> filteredParams = filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("status") || key.equals("business") ||
                            key.equals("route") || key.equals("region");
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

    private List<Long> fetchSourceIds(String query) {
        List<SourceDTO> sources = sourceClient.findByNameContaining(query);
        return sources.stream()
                .map(SourceDTO::getId)
                .toList();
    }

    private Page<Sale> fetchSales(String query, Map<String, List<String>> filterParams,
                                  List<Long> clientIds, List<Long> sourceIds, Pageable pageable) {
        Specification<Sale> spec = new SaleSpecification(query, filterParams, clientIds, sourceIds);
        return saleRepository.findAll(spec, pageable);
    }

    private SalePageDTO mapToSalePageDTO(Sale sale, Map<Long, ClientDTO> clientMap) {
        SalePageDTO dto = new SalePageDTO();
        dto.setId(sale.getId());
        dto.setUserId(sale.getUser());
        dto.setClient(clientMap.get(sale.getClient()));
        dto.setSourceId(sale.getSource());
        dto.setProductId(sale.getProduct());
        dto.setQuantity(sale.getQuantity());
        dto.setUnitPrice(sale.getUnitPrice());
        dto.setTotalPrice(sale.getTotalPrice());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setCurrency(sale.getCurrency());
        dto.setTransactionId(sale.getTransaction());
        dto.setCreatedAt(sale.getCreatedAt());
        dto.setUpdatedAt(sale.getUpdatedAt());
        return dto;
    }

    @Override
    public List<Sale> getSalesByClientId(Long clientId) {
        return saleRepository.findByClient(clientId);
    }
}
