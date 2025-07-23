package org.example.purchaseservice.services.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.SourceClient;
import org.example.purchaseservice.models.ClientData;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.purchase.PurchasePageDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.example.purchaseservice.spec.PurchaseSpecification;
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
public class PurchaseSearchService implements IPurchaseSearchService {
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final SourceClient sourceClient;

    @Override
    public PageResponse<PurchasePageDTO> searchPurchase(String query, Pageable pageable,
                                                        Map<String, List<String>> filterParams) {

        ClientData clientData = fetchClientData(query, filterParams);
        List<Long> clientIds = clientData.clientIds();
        Map<Long, ClientDTO> clientMap = clientData.clientMap();

        List<Long> sourceIds = (query == null || query.trim().isEmpty()) ? null : fetchSourceIds(query);

        Page<Purchase> purchasePage = fetchPurchases(query, filterParams, clientIds, sourceIds, pageable);

        List<PurchasePageDTO> purchaseDTOs = purchasePage.getContent().stream()
                .map(purchase -> mapToPurchasePageDTO(purchase, clientMap))
                .collect(Collectors.toList());

        return new PageResponse<>(purchasePage.getNumber(), purchasePage.getSize(), purchasePage.getTotalElements(),
                purchasePage.getTotalPages(), purchaseDTOs);
    }

    private ClientData fetchClientData(String query, Map<String, List<String>> filterParams) {
        Map<String, List<String>> filteredParams = filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("status") || key.equals("business") ||
                            key.equals("route") || key.equals("region") || key.equals("clientProduct");
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

    private Page<Purchase> fetchPurchases(String query, Map<String, List<String>> filterParams,
                                          List<Long> clientIds, List<Long> sourceIds, Pageable pageable) {
        Specification<Purchase> spec = new PurchaseSpecification(query, filterParams, clientIds, sourceIds);

        return purchaseRepository.findAll(spec, pageable);
    }

    private PurchasePageDTO mapToPurchasePageDTO(Purchase purchase, Map<Long, ClientDTO> clientMap) {
        PurchasePageDTO dto = new PurchasePageDTO();
        dto.setId(purchase.getId());
        dto.setUserId(purchase.getUser());
        dto.setClient(clientMap.get(purchase.getClient()));
        dto.setSourceId(purchase.getSource());
        dto.setProductId(purchase.getProduct());
        dto.setQuantity(purchase.getQuantity());
        dto.setUnitPrice(purchase.getUnitPrice());
        dto.setTotalPrice(purchase.getTotalPrice());
        dto.setPaymentMethod(purchase.getPaymentMethod());
        dto.setCurrency(purchase.getCurrency());
        dto.setExchangeRate(purchase.getExchangeRate());
        dto.setTransactionId(purchase.getTransaction());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setUpdatedAt(purchase.getUpdatedAt());
        return dto;
    }

    @Override
    public List<Purchase> getPurchasesByClientId(Long clientId) {
        return purchaseRepository.findByClient(clientId);
    }

    @Override
    public List<Purchase> searchForWarehouse(Map<String, List<String>> filters) {
        Specification<Purchase> spec = new PurchaseSpecification(null, filters, null, null);

        return purchaseRepository.findAll(spec);
    }

}
