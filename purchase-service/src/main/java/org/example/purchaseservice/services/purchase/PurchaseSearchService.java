package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.PurchaseMapper;
import org.example.purchaseservice.models.ClientData;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.purchase.PurchasePageDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.example.purchaseservice.spec.PurchaseFilterBuilder;
import org.example.purchaseservice.spec.PurchaseSearchPredicateBuilder;
import org.example.purchaseservice.spec.PurchaseSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSearchService implements IPurchaseSearchService {
    
    private final PurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;
    private final PurchaseSearchValidator validator;
    private final PurchaseFilterProcessor filterProcessor;
    private final PurchaseClientDataFetcher clientDataFetcher;
    private final PurchaseReceivedStatusBuilder receivedStatusBuilder;
    private final PurchaseReportGenerator reportGenerator;
    private final PurchaseFilterBuilder filterBuilder;
    private final PurchaseSearchPredicateBuilder searchPredicateBuilder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchasePageDTO> searchPurchase(String query, @NonNull Pageable pageable,
                                                        Map<String, List<String>> filterParams) {
        validator.validateSearchRequest(pageable, query);
        
        PurchaseFilterProcessor.SearchFilters filters = filterProcessor.extractFilters(filterParams);
        
        ClientData clientData = clientDataFetcher.fetchClientData(query, filters.clientFilterParams(), filters.clientTypeId());
        List<Long> sourceIds = clientDataFetcher.resolveSourceIds(query, filters.clientFilterParams(), filters.clientTypeId());
        
        Page<Purchase> purchasePage = fetchPurchases(query, filters.purchaseFilterParams(), 
                clientData.clientIds(), sourceIds, pageable);
        
        List<Purchase> purchases = purchasePage.getContent();
        
        Map<String, Boolean> receivedStatusMap = receivedStatusBuilder.buildReceivedStatusMap(purchases);
        
        Set<Long> requiredClientIds = purchases.stream()
                .map(Purchase::getClient)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, ClientDTO> clientMap = clientDataFetcher.fetchClientsByIds(requiredClientIds);
        
        List<PurchasePageDTO> purchaseDTOs = mapPurchasesToDTOs(purchases, clientMap, receivedStatusMap);
        
        return buildPageResponse(purchasePage, purchaseDTOs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByClientId(@NonNull Long clientId) {
        return purchaseRepository.findByClient(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> searchForWarehouse(@NonNull Map<String, List<String>> filters) {
        Specification<Purchase> spec = new PurchaseSpecification(null, filters, null, null, filterBuilder, searchPredicateBuilder);
        return purchaseRepository.findAll(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseReportDTO generateReport(String query, Map<String, List<String>> filterParams) {
        PurchaseFilterProcessor.SearchFilters filters = filterProcessor.extractFilters(filterParams);
        
        ClientData clientData = clientDataFetcher.fetchClientData(query, filters.clientFilterParams(), filters.clientTypeId());
        List<Long> sourceIds = clientDataFetcher.resolveSourceIds(query, filters.clientFilterParams(), filters.clientTypeId());
        
        Specification<Purchase> spec = new PurchaseSpecification(query, filters.purchaseFilterParams(), 
                clientData.clientIds(), sourceIds, filterBuilder, searchPredicateBuilder);
        List<Purchase> purchases = purchaseRepository.findAll(spec);
        
        return reportGenerator.generateReport(purchases);
    }
    
    private Page<Purchase> fetchPurchases(String query, @NonNull Map<String, List<String>> filterParams,
                                          List<Long> clientIds, List<Long> sourceIds, @NonNull Pageable pageable) {
        if (clientIds != null && clientIds.isEmpty()) {
            return Page.empty(pageable);
        }

        if (sourceIds != null && sourceIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        Specification<Purchase> spec = new PurchaseSpecification(query, filterParams, clientIds, sourceIds, filterBuilder, searchPredicateBuilder);
        return purchaseRepository.findAll(spec, pageable);
    }

    private List<PurchasePageDTO> mapPurchasesToDTOs(@NonNull List<Purchase> purchases, 
                                                      @NonNull Map<Long, ClientDTO> clientMap,
                                                      @NonNull Map<String, Boolean> receivedStatusMap) {
        return purchases.stream()
                .map(purchase -> {
                    ClientDTO client = clientMap.get(purchase.getClient());
                    PurchasePageDTO dto = purchaseMapper.toPurchasePageDTO(purchase, client);
                    String key = receivedStatusBuilder.buildReceivedStatusKey(purchase);
                    dto.setIsReceived(receivedStatusMap.getOrDefault(key, false));
                    return dto;
                })
                .toList();
    }
    
    private PageResponse<PurchasePageDTO> buildPageResponse(@NonNull Page<Purchase> purchasePage, 
                                                           @NonNull List<PurchasePageDTO> purchaseDTOs) {
        return new PageResponse<>(purchasePage.getNumber(), purchasePage.getSize(), 
                purchasePage.getTotalElements(), purchasePage.getTotalPages(), purchaseDTOs);
    }
}
