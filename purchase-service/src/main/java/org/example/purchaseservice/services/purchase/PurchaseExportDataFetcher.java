package org.example.purchaseservice.services.purchase;

import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.clients.ClientTypeFieldApiClient;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientIdsRequest;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.purchaseservice.models.dto.clienttype.FieldIdsRequest;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.ISourceService;
import org.example.purchaseservice.services.impl.IProductService;
import org.example.purchaseservice.services.impl.IUserService;
import org.example.purchaseservice.spec.PurchaseFilterBuilder;
import org.example.purchaseservice.spec.PurchaseSearchPredicateBuilder;
import org.example.purchaseservice.spec.PurchaseSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseExportDataFetcher {
    
    private static final String ALL_PRODUCTS_FILTER = "all";
    private static final int BATCH_SIZE = 100;
    
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final ClientTypeFieldApiClient clientTypeFieldApiClient;
    private final IUserService userService;
    private final ISourceService sourceService;
    private final IProductService productService;
    private final PurchaseFilterBuilder filterBuilder;
    private final PurchaseSearchPredicateBuilder searchPredicateBuilder;
    
    public record SearchContext(
            Long clientTypeId,
            Map<String, List<String>> clientFilterParams,
            List<Long> clientIds,
            List<Long> sourceIds
    ) {}
    
    public record FilterIds(
            List<SourceDTO> sourceDTOs, List<Long> sourceIds,
            List<Product> productDTOs, List<Long> productIds,
            List<UserDTO> userDTOs, List<Long> userIds
    ) {}
    
    public List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        try {
            Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
            Map<String, List<String>> filteredParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, true);
            ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
            List<ClientDTO> clients = clientApiClient.searchClients(clientRequest).getBody();
            return clients != null ? clients : Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error fetching client IDs: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error fetching client IDs: query={}, error={}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    public SearchContext prepareSearchContext(String query, Map<String, List<String>> filterParams, 
                                               @NonNull List<ClientDTO> clients) {
        Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, false);
        List<Long> clientIds = clients.stream()
                .filter(client -> client != null && client.getId() != null)
                .map(ClientDTO::getId)
                .toList();
        
        List<Long> sourceIds = resolveSourceIds(query, clientFilterParams, clientTypeId);
        
        return new SearchContext(clientTypeId, clientFilterParams, clientIds, sourceIds);
    }
    
    public List<Long> resolveSourceIds(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            return fetchSourceIds(query);
        }
        return null;
    }
    
    private List<Long> fetchSourceIds(@NonNull String query) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<SourceDTO> sources = sourceService.findByNameContaining(query);
            return sources.stream()
                    .filter(Objects::nonNull)
                    .map(SourceDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    public List<Purchase> fetchPurchases(String query, Map<String, List<String>> filterParams, List<Long> clientIds,
                                          List<Long> sourceIds, Sort sort) {
        Specification<Purchase> spec = (root, querySpec, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            } else {
                return criteriaBuilder.disjunction();
            }

            Specification<Purchase> purchaseSpec = new PurchaseSpecification(query, filterParams, clientIds, sourceIds, filterBuilder, searchPredicateBuilder);
            predicates.add(purchaseSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return purchaseRepository.findAll(spec, sort);
    }
    
    public FilterIds createFilterIds() {
        List<Product> products = productService.getAllProducts(ALL_PRODUCTS_FILTER);
        List<Long> productIds = products.stream()
                .filter(product -> product != null && product.getId() != null)
                .map(Product::getId)
                .toList();

        List<UserDTO> userDTOs = userService.getAllUsers();
        List<Long> userIds = userDTOs.stream()
                .filter(user -> user != null && user.getId() != null)
                .map(UserDTO::getId)
                .toList();

        return new FilterIds(
                Collections.emptyList(), Collections.emptyList(),
                products, productIds,
                userDTOs, userIds);
    }
    
    public FilterIds buildUpdatedFilterIds(@NonNull List<Purchase> purchaseList, @NonNull FilterIds baseFilterIds) {
        List<SourceDTO> sourceDTOs = fetchSourceDTOs(purchaseList);
        return new FilterIds(
                sourceDTOs,
                sourceDTOs.stream()
                        .filter(Objects::nonNull)
                        .map(SourceDTO::getId)
                        .filter(Objects::nonNull)
                        .toList(),
                baseFilterIds.productDTOs(),
                baseFilterIds.productIds(),
                baseFilterIds.userDTOs(),
                baseFilterIds.userIds()
        );
    }
    
    private List<SourceDTO> fetchSourceDTOs(@NonNull List<Purchase> purchases) {
        Set<Long> sourceIds = purchases.stream()
                .map(Purchase::getSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (sourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SourceDTO> sourceDTOs = new ArrayList<>();
        for (Long sourceId : sourceIds) {
            try {
                SourceDTO sourceDTO = sourceService.getSourceName(sourceId);
                if (sourceDTO != null) {
                    sourceDTOs.add(sourceDTO);
                }
            } catch (Exception _) {
            }
        }

        return sourceDTOs;
    }
    
    public Map<Long, ClientDTO> fetchClientMap(@NonNull List<ClientDTO> clients) {
        return clients.stream()
                .filter(client -> client != null && client.getId() != null)
                .collect(Collectors.toMap(ClientDTO::getId, client -> client, (existing, _) -> existing));
    }
    
    public Map<Long, List<ClientFieldValueDTO>> fetchClientFieldValues(@NonNull List<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, List<ClientFieldValueDTO>> result = new HashMap<>();
        
        for (int i = 0; i < clientIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, clientIds.size());
            List<Long> batch = clientIds.subList(i, endIndex);
            
            try {
                ClientIdsRequest request = new ClientIdsRequest(batch);
                Map<Long, List<ClientFieldValueDTO>> batchResult = clientApiClient.getClientFieldValuesBatch(request).getBody();
                if (batchResult != null) {
                    result.putAll(batchResult);
                }
            } catch (FeignException e) {
                log.error("Feign error fetching field values batch for clients: status={}, error={}", 
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error fetching field values batch for clients: error={}", e.getMessage(), e);
            }
        }
        
        return result;
    }
    
    public List<SourceDTO> fetchClientSourceDTOs(@NonNull List<ClientDTO> clients) {
        Map<Long, SourceDTO> uniqueSources = new HashMap<>();
        
        for (ClientDTO client : clients) {
            try {
                java.lang.reflect.Method getSourceMethod = client.getClass().getMethod("getSource");
                Object sourceObj = getSourceMethod.invoke(client);
                if (sourceObj != null) {
                    SourceDTO sourceDTO = (SourceDTO) sourceObj;
                    if (sourceDTO.getId() != null) {
                        uniqueSources.put(sourceDTO.getId(), sourceDTO);
                    }
                }
            } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException | ClassCastException _) {
            }
            
            String sourceId = client.getSourceId();
            if (sourceId != null && !sourceId.trim().isEmpty()) {
                try {
                    Long sourceIdLong = Long.parseLong(sourceId.trim());
                    if (!uniqueSources.containsKey(sourceIdLong)) {
                        SourceDTO sourceDTO = sourceService.getSourceName(sourceIdLong);
                        if (sourceDTO != null) {
                            uniqueSources.put(sourceIdLong, sourceDTO);
                        }
                    }
                } catch (Exception _) {
                }
            }
        }

        return new ArrayList<>(uniqueSources.values());
    }
    
    public Map<Long, ClientTypeFieldDTO> fetchClientTypeFields(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, ClientTypeFieldDTO> result = new HashMap<>();
        
        for (int i = 0; i < fieldIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, fieldIds.size());
            List<Long> batch = fieldIds.subList(i, endIndex);
            
            try {
                FieldIdsRequest request = new FieldIdsRequest(batch);
                List<ClientTypeFieldDTO> fields = clientTypeFieldApiClient.getFieldsByIds(request).getBody();
                if (fields != null) {
                    fields.stream()
                            .filter(Objects::nonNull)
                            .forEach(field -> result.putIfAbsent(field.getId(), field));
                }
            } catch (FeignException e) {
                log.error("Feign error fetching client type fields: status={}, error={}", 
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error fetching client type fields: error={}", e.getMessage(), e);
            }
        }
        
        return result;
    }
    
    public List<SourceDTO> mergeSourceDTOs(@NonNull List<SourceDTO> purchaseSources, @NonNull List<SourceDTO> clientSources) {
        Map<Long, SourceDTO> mergedMap = new HashMap<>();
        purchaseSources.forEach(source -> {
            if (source != null && source.getId() != null) {
                mergedMap.put(source.getId(), source);
            }
        });
        clientSources.forEach(source -> {
            if (source != null && source.getId() != null) {
                mergedMap.putIfAbsent(source.getId(), source);
            }
        });
        return new ArrayList<>(mergedMap.values());
    }
}
