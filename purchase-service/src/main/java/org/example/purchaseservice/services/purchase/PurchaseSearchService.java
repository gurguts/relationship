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
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.example.purchaseservice.spec.PurchaseSpecification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSearchService implements IPurchaseSearchService {
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final SourceClient sourceClient;
    private final WarehouseReceiptRepository warehouseReceiptRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchasePageDTO> searchPurchase(String query, Pageable pageable,
                                                        Map<String, List<String>> filterParams) {

        Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);

        Map<String, List<String>> clientFilterParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, false);

        ClientData clientData = fetchClientData(query, clientFilterParams, clientTypeId);
        List<Long> clientIds = clientData.clientIds();
        Map<Long, ClientDTO> clientMap = clientData.clientMap();

        Map<String, List<String>> purchaseFilterParams = filterParams != null ? filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return !key.equals("clientTypeId") && 
                           !key.equals("clientProduct") &&
                           !key.equals("clientSource") &&
                           !key.equals("clientCreatedAtFrom") && !key.equals("clientCreatedAtTo") &&
                           !key.equals("clientUpdatedAtFrom") && !key.equals("clientUpdatedAtTo") &&
                           !key.startsWith("field");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) : Collections.emptyMap();

        List<Long> sourceIds = null;
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            sourceIds = fetchSourceIds(query);
        }

        Page<Purchase> purchasePage = fetchPurchases(query, purchaseFilterParams, clientIds, sourceIds, pageable);

        List<Purchase> purchases = purchasePage.getContent();
        Map<String, Boolean> receivedStatusMap = buildReceivedStatusMap(purchases);

        List<PurchasePageDTO> purchaseDTOs = purchases.stream()
                .map(purchase -> {
                    PurchasePageDTO dto = mapToPurchasePageDTO(purchase, clientMap);
                    String key = purchase.getUser() + "_" + purchase.getProduct() + "_" + 
                            (purchase.getCreatedAt() != null ? purchase.getCreatedAt().toString() : "");
                    dto.setIsReceived(receivedStatusMap.getOrDefault(key, false));
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageResponse<>(purchasePage.getNumber(), purchasePage.getSize(), purchasePage.getTotalElements(),
                purchasePage.getTotalPages(), purchaseDTOs);
    }

    private ClientData fetchClientData(String query, Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, clientFilterParams, clientTypeId);
        List<ClientDTO> foundClients = clientApiClient.searchClients(clientRequest).getBody();
        if (foundClients == null) {
            foundClients = Collections.emptyList();
        }

        if (!clientFilterParams.isEmpty() || clientTypeId != null || (query != null && !query.trim().isEmpty())) {
            Map<Long, ClientDTO> clientMap = foundClients.stream()
                    .collect(Collectors.toMap(ClientDTO::getId, client -> client));
            
            List<Long> clientIds = foundClients.stream()
                    .map(ClientDTO::getId)
                    .collect(Collectors.toList());
            
            return new ClientData(clientIds, clientMap);
        } else {
            ClientSearchRequest allClientsRequest = new ClientSearchRequest(null, Collections.emptyMap(), clientTypeId);
            List<ClientDTO> allClients = clientApiClient.searchClients(allClientsRequest).getBody();
            if (allClients == null) {
                allClients = Collections.emptyList();
            }
            Map<Long, ClientDTO> clientMap = allClients.stream()
                    .collect(Collectors.toMap(ClientDTO::getId, client -> client));
            
            return new ClientData(null, clientMap);
        }
    }

    private List<Long> fetchSourceIds(String query) {
        List<SourceDTO> sources = sourceClient.findByNameContaining(query).getBody();
        if (sources == null) {
            sources = Collections.emptyList();
        }
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
        dto.setTotalPriceEur(purchase.getTotalPriceEur());
        dto.setPaymentMethod(purchase.getPaymentMethod());
        dto.setCurrency(purchase.getCurrency());
        dto.setExchangeRate(purchase.getExchangeRate());
        dto.setTransactionId(purchase.getTransaction());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setUpdatedAt(purchase.getUpdatedAt());
        dto.setComment(purchase.getComment());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByClientId(Long clientId) {
        return purchaseRepository.findByClient(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> searchForWarehouse(Map<String, List<String>> filters) {
        Specification<Purchase> spec = new PurchaseSpecification(null, filters, null, null);

        return purchaseRepository.findAll(spec);
    }

    private Map<String, Boolean> buildReceivedStatusMap(List<Purchase> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<Long> userIds = purchases.stream()
                .map(Purchase::getUser)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<Long> productIds = purchases.stream()
                .map(Purchase::getProduct)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (userIds.isEmpty() || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Specification<WarehouseReceipt> spec = (root, query, cb) -> {
            Predicate userIdPredicate = root.get("userId").in(userIds);
            Predicate productIdPredicate = root.get("productId").in(productIds);
            return cb.and(userIdPredicate, productIdPredicate);
        };
        
        List<WarehouseReceipt> receipts = warehouseReceiptRepository.findAll(spec);
        
        Map<String, Boolean> statusMap = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getUser() == null || purchase.getProduct() == null || purchase.getCreatedAt() == null) {
                statusMap.put(purchase.getUser() + "_" + purchase.getProduct() + "_" + 
                        (purchase.getCreatedAt() != null ? purchase.getCreatedAt().toString() : ""), false);
                continue;
            }
            
            boolean isReceived = receipts.stream()
                    .anyMatch(receipt -> receipt.getUserId().equals(purchase.getUser()) &&
                            receipt.getProductId().equals(purchase.getProduct()) &&
                            receipt.getCreatedAt() != null &&
                            !receipt.getCreatedAt().isBefore(purchase.getCreatedAt()));
            
            String key = purchase.getUser() + "_" + purchase.getProduct() + "_" + purchase.getCreatedAt().toString();
            statusMap.put(key, isReceived);
        }
        
        return statusMap;
    }

}
