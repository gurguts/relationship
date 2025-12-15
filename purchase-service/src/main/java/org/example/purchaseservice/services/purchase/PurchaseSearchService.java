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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
    private final WarehouseReceiptRepository warehouseReceiptRepository;

    @Override
    public PageResponse<PurchasePageDTO> searchPurchase(String query, Pageable pageable,
                                                        Map<String, List<String>> filterParams) {

        Long clientTypeId = null;
        if (filterParams != null && filterParams.containsKey("clientTypeId") && filterParams.get("clientTypeId") != null 
                && !filterParams.get("clientTypeId").isEmpty()) {
            try {
                clientTypeId = Long.parseLong(filterParams.get("clientTypeId").get(0));
            } catch (NumberFormatException e) {
                log.warn("Invalid clientTypeId in filterParams: {}", filterParams.get("clientTypeId"));
            }
        }

        Map<String, List<String>> clientFilterParams = filterParams != null ? filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("clientProduct") ||
                            key.equals("clientSource") ||
                            key.equals("clientCreatedAtFrom") || key.equals("clientCreatedAtTo") ||
                            key.equals("clientUpdatedAtFrom") || key.equals("clientUpdatedAtTo") ||
                            key.startsWith("field");
                })
                .collect(Collectors.toMap(
                    entry -> {
                        String key = entry.getKey();
                        if (key.equals("clientSource")) return "source";
                        if (key.equals("clientCreatedAtFrom")) return "createdAtFrom";
                        if (key.equals("clientCreatedAtTo")) return "createdAtTo";
                        if (key.equals("clientUpdatedAtFrom")) return "updatedAtFrom";
                        if (key.equals("clientUpdatedAtTo")) return "updatedAtTo";
                        return key;
                    },
                    Map.Entry::getValue
                )) : Collections.emptyMap();

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

        List<PurchasePageDTO> purchaseDTOs = purchasePage.getContent().stream()
                .map(purchase -> {
                    PurchasePageDTO dto = mapToPurchasePageDTO(purchase, clientMap);
                    dto.setIsReceived(isPurchaseReceived(purchase));
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageResponse<>(purchasePage.getNumber(), purchasePage.getSize(), purchasePage.getTotalElements(),
                purchasePage.getTotalPages(), purchaseDTOs);
    }

    private ClientData fetchClientData(String query, Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, clientFilterParams, clientTypeId);
        List<ClientDTO> foundClients = clientApiClient.searchClients(clientRequest);

        if (!clientFilterParams.isEmpty() || clientTypeId != null || (query != null && !query.trim().isEmpty())) {
            Map<Long, ClientDTO> clientMap = foundClients.stream()
                    .collect(Collectors.toMap(ClientDTO::getId, client -> client));
            
            List<Long> clientIds = foundClients.stream()
                    .map(ClientDTO::getId)
                    .collect(Collectors.toList());
            
            return new ClientData(clientIds, clientMap);
        } else {
            ClientSearchRequest allClientsRequest = new ClientSearchRequest(null, Collections.emptyMap(), clientTypeId);
            List<ClientDTO> allClients = clientApiClient.searchClients(allClientsRequest);
            Map<Long, ClientDTO> clientMap = allClients.stream()
                    .collect(Collectors.toMap(ClientDTO::getId, client -> client));
            
            return new ClientData(null, clientMap);
        }
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
    public List<Purchase> getPurchasesByClientId(Long clientId) {
        return purchaseRepository.findByClient(clientId);
    }

    @Override
    public List<Purchase> searchForWarehouse(Map<String, List<String>> filters) {
        Specification<Purchase> spec = new PurchaseSpecification(null, filters, null, null);

        return purchaseRepository.findAll(spec);
    }

    private boolean isPurchaseReceived(org.example.purchaseservice.models.Purchase purchase) {
        if (purchase == null || purchase.getUser() == null || purchase.getProduct() == null 
                || purchase.getCreatedAt() == null) {
            return false;
        }

        Specification<WarehouseReceipt> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("userId"), purchase.getUser()),
                cb.equal(root.get("productId"), purchase.getProduct()),
                cb.greaterThanOrEqualTo(root.get("createdAt"), purchase.getCreatedAt())
        );
        
        List<WarehouseReceipt> receipts = warehouseReceiptRepository.findAll(spec);
        
        return !receipts.isEmpty();
    }

}
