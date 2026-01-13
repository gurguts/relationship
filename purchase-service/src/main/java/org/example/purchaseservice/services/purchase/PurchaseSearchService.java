package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.PurchaseMapper;
import org.example.purchaseservice.services.source.SourceService;
import org.example.purchaseservice.models.ClientData;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.UserProductIds;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.purchase.PurchasePageDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.example.purchaseservice.services.user.UserService;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.spec.PurchaseSpecification;
import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import org.example.purchaseservice.utils.FilterUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSearchService implements IPurchaseSearchService {
    
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final String FILTER_KEY_CLIENT_PRODUCT = "clientProduct";
    private static final String FILTER_KEY_CLIENT_SOURCE = "clientSource";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_FROM = "clientCreatedAtFrom";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_TO = "clientCreatedAtTo";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_FROM = "clientUpdatedAtFrom";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_TO = "clientUpdatedAtTo";
    private static final String FILTER_PREFIX_FIELD = "field";
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int MAX_QUERY_LENGTH = 255;

    private static final Set<String> EXCLUDED_FILTER_KEYS = Set.of(
            FILTER_KEY_CLIENT_TYPE_ID,
            FILTER_KEY_CLIENT_PRODUCT,
            FILTER_KEY_CLIENT_SOURCE,
            FILTER_KEY_CLIENT_CREATED_AT_FROM,
            FILTER_KEY_CLIENT_CREATED_AT_TO,
            FILTER_KEY_CLIENT_UPDATED_AT_FROM,
            FILTER_KEY_CLIENT_UPDATED_AT_TO
    );
    
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final SourceService sourceService;
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final PurchaseMapper purchaseMapper;
    private final UserService userService;
    private final ProductRepository productRepository;

    private record SearchFilters(
            Long clientTypeId,
            Map<String, List<String>> clientFilterParams,
            Map<String, List<String>> purchaseFilterParams
    ) {}

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchasePageDTO> searchPurchase(String query, @NonNull Pageable pageable,
                                                        Map<String, List<String>> filterParams) {
        validateSearchRequest(pageable, query);
        
        SearchFilters filters = extractFilters(filterParams);
        
        ClientData clientData = fetchClientData(query, filters.clientFilterParams(), filters.clientTypeId());
        List<Long> sourceIds = resolveSourceIds(query, filters.clientFilterParams(), filters.clientTypeId());
        
        Page<Purchase> purchasePage = fetchPurchases(query, filters.purchaseFilterParams(), 
                clientData.clientIds(), sourceIds, pageable);
        
        List<Purchase> purchases = purchasePage.getContent();
        
        Map<String, Boolean> receivedStatusMap = buildReceivedStatusMap(purchases);
        
        Set<Long> requiredClientIds = purchases.stream()
                .map(Purchase::getClient)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, ClientDTO> clientMap = fetchClientsByIds(requiredClientIds);
        
        List<PurchasePageDTO> purchaseDTOs = mapPurchasesToDTOs(purchases, clientMap, receivedStatusMap);
        
        return buildPageResponse(purchasePage, purchaseDTOs);
    }

    private void validateSearchRequest(@NonNull Pageable pageable, String query) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Page size cannot exceed %d. Requested size: %d", 
                            MAX_PAGE_SIZE, pageable.getPageSize()));
        }
        
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException(
                    String.format("Page number cannot be negative. Requested page: %d", 
                            pageable.getPageNumber()));
        }
        
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Query length cannot exceed %d characters. Query length: %d", 
                            MAX_QUERY_LENGTH, query.length()));
        }
    }

    private SearchFilters extractFilters(Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = FilterUtils.filterClientParams(filterParams, false);
        Map<String, List<String>> purchaseFilterParams = filterPurchaseParams(filterParams);
        return new SearchFilters(clientTypeId, clientFilterParams, purchaseFilterParams);
    }

    private List<Long> resolveSourceIds(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            return fetchSourceIds(query);
        }
        return null;
    }

    private PageResponse<PurchasePageDTO> buildPageResponse(@NonNull Page<Purchase> purchasePage, 
                                                           @NonNull List<PurchasePageDTO> purchaseDTOs) {
        return new PageResponse<>(purchasePage.getNumber(), purchasePage.getSize(), 
                purchasePage.getTotalElements(), purchasePage.getTotalPages(), purchaseDTOs);
    }

    private Map<String, List<String>> filterPurchaseParams(Map<String, List<String>> filterParams) {
        if (filterParams == null || filterParams.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return filterParams.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> !isExcludedFilterKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    private boolean isExcludedFilterKey(@NonNull String key) {
        return EXCLUDED_FILTER_KEYS.contains(key) || key.startsWith(FILTER_PREFIX_FIELD);
    }
    
    private ClientData fetchClientData(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        try {
            if (hasClientFilters(clientFilterParams, query)) {
                ClientData clientData = fetchClientIdsWithFilters(query, clientFilterParams, clientTypeId);
                if (clientData.clientIds() != null && clientData.clientIds().isEmpty() && 
                    StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                    return new ClientData(null, Collections.emptyMap());
                }
                return clientData;
            } else {
                return new ClientData(null, Collections.emptyMap());
            }
        } catch (FeignException e) {
            log.error("Feign error fetching client data: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            if (StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                return new ClientData(null, Collections.emptyMap());
            }
            return new ClientData(Collections.emptyList(), Collections.emptyMap());
        } catch (Exception e) {
            log.error("Unexpected error fetching client data: query={}, error={}", query, e.getMessage(), e);
            if (StringUtils.hasText(query) && clientFilterParams.isEmpty()) {
                return new ClientData(null, Collections.emptyMap());
            }
            return new ClientData(Collections.emptyList(), Collections.emptyMap());
        }
    }

    private boolean hasClientFilters(@NonNull Map<String, List<String>> clientFilterParams, String query) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasClientFilters = !clientFilterParams.isEmpty();
        return hasClientFilters || hasQuery;
    }

    private ClientData fetchClientIdsWithFilters(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, clientFilterParams, clientTypeId);
        List<Long> clientIds = clientApiClient.searchClientIds(clientRequest).getBody();
        if (clientIds == null) {
            clientIds = Collections.emptyList();
        }
        return new ClientData(clientIds, Collections.emptyMap());
    }

    private Map<Long, ClientDTO> fetchClientsByIds(@NonNull Set<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            List<Long> clientIdsList = new ArrayList<>(clientIds);
            List<ClientDTO> clients = clientApiClient.getClientsByIds(clientIdsList).getBody();
            if (clients == null) {
                clients = Collections.emptyList();
            }
            return buildClientMap(clients);
        } catch (FeignException e) {
            log.error("Feign error fetching clients by IDs: status={}, error={}", e.status(), e.getMessage(), e);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Unexpected error fetching clients by IDs: error={}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    private Map<Long, ClientDTO> buildClientMap(@NonNull List<ClientDTO> clients) {
        return clients.stream()
                .filter(Objects::nonNull)
                .filter(client -> client.getId() != null)
                .collect(Collectors.toMap(
                        ClientDTO::getId,
                        client -> client,
                        (existing, _) -> existing
                ));
    }

    private List<Long> fetchSourceIds(@NonNull String query) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SourceDTO> sources = sourceService.findByNameContaining(query);
        return sources.stream()
                .map(SourceDTO::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Page<Purchase> fetchPurchases(String query, @NonNull Map<String, List<String>> filterParams,
                                          List<Long> clientIds, List<Long> sourceIds, @NonNull Pageable pageable) {
        if (clientIds != null && clientIds.isEmpty()) {
            return Page.empty(pageable);
        }

        if (sourceIds != null && sourceIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        Specification<Purchase> spec = new PurchaseSpecification(query, filterParams, clientIds, sourceIds);
        return purchaseRepository.findAll(spec, pageable);
    }

    private List<PurchasePageDTO> mapPurchasesToDTOs(@NonNull List<Purchase> purchases, 
                                                      @NonNull Map<Long, ClientDTO> clientMap,
                                                      @NonNull Map<String, Boolean> receivedStatusMap) {
        return purchases.stream()
                .map(purchase -> {
                    ClientDTO client = clientMap.get(purchase.getClient());
                    PurchasePageDTO dto = purchaseMapper.toPurchasePageDTO(purchase, client);
                    String key = buildReceivedStatusKey(purchase);
                    dto.setIsReceived(receivedStatusMap.getOrDefault(key, false));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByClientId(@NonNull Long clientId) {
        return purchaseRepository.findByClient(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> searchForWarehouse(@NonNull Map<String, List<String>> filters) {
        Specification<Purchase> spec = new PurchaseSpecification(null, filters, null, null);
        return purchaseRepository.findAll(spec);
    }

    private Map<String, Boolean> buildReceivedStatusMap(@NonNull List<Purchase> purchases) {
        if (purchases.isEmpty()) {
            return Collections.emptyMap();
        }
        
        UserProductIds userProductIds = extractUserAndProductIds(purchases);
        if (userProductIds.userIds().isEmpty() || userProductIds.productIds().isEmpty()) {
            return Collections.emptyMap();
        }
        
        LocalDateTime minPurchaseDate = purchases.stream()
                .map(Purchase::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        
        List<WarehouseReceipt> receipts = loadWarehouseReceipts(userProductIds.userIds(), userProductIds.productIds(), minPurchaseDate);
        
        Map<String, NavigableSet<LocalDateTime>> receiptDatesByKey = groupReceiptsByKey(receipts);

        return buildStatusMap(purchases, receiptDatesByKey);
    }

    private UserProductIds extractUserAndProductIds(@NonNull List<Purchase> purchases) {
        List<Long> userIds = purchases.stream()
                .map(Purchase::getUser)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        List<Long> productIds = purchases.stream()
                .map(Purchase::getProduct)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        return new UserProductIds(userIds, productIds);
    }

    private List<WarehouseReceipt> loadWarehouseReceipts(@NonNull List<Long> userIds, @NonNull List<Long> productIds, LocalDateTime minPurchaseDate) {
        Specification<WarehouseReceipt> spec = (root, _, cb) -> {
            Predicate userIdPredicate = root.get("userId").in(userIds);
            Predicate productIdPredicate = root.get("productId").in(productIds);
            Predicate basePredicate = cb.and(userIdPredicate, productIdPredicate);
            
            if (minPurchaseDate != null) {
                Predicate datePredicate = cb.greaterThanOrEqualTo(root.get("createdAt"), minPurchaseDate);
                return cb.and(basePredicate, datePredicate);
            }
            
            return basePredicate;
        };
        return warehouseReceiptRepository.findAll(spec);
    }

    private Map<String, NavigableSet<LocalDateTime>> groupReceiptsByKey(@NonNull List<WarehouseReceipt> receipts) {
        return receipts.stream()
                .filter(receipt -> receipt.getUserId() != null && 
                                  receipt.getProductId() != null && 
                                  receipt.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        receipt -> buildReceiptKey(receipt.getUserId(), receipt.getProductId()),
                        Collectors.mapping(
                                WarehouseReceipt::getCreatedAt,
                                Collectors.toCollection(TreeSet::new)
                        )
                ));
    }

    private Map<String, Boolean> buildStatusMap(@NonNull List<Purchase> purchases, 
                                                @NonNull Map<String, NavigableSet<LocalDateTime>> receiptDatesByKey) {
        Map<String, Boolean> statusMap = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase == null) {
                continue;
            }

            Long userId = purchase.getUser();
            Long productId = purchase.getProduct();
            LocalDateTime createdAt = purchase.getCreatedAt();
            
            if (userId == null || productId == null || createdAt == null) {
                String key = buildReceivedStatusKey(purchase);
                statusMap.put(key, false);
                continue;
            }

            String receiptKey = buildReceiptKey(userId, productId);
            NavigableSet<LocalDateTime> receiptDates = receiptDatesByKey.get(receiptKey);
            boolean isReceived = receiptDates != null && receiptDates.ceiling(createdAt) != null;
            
            String key = buildReceivedStatusKey(purchase);
            statusMap.put(key, isReceived);
        }
        return statusMap;
    }
    
    private String buildReceivedStatusKey(@NonNull Purchase purchase) {
        Long userId = purchase.getUser();
        Long productId = purchase.getProduct();
        LocalDateTime createdAt = purchase.getCreatedAt();

        String userIdStr = userId != null ? userId.toString() : "null";
        String productIdStr = productId != null ? productId.toString() : "null";
        String createdAtStr = createdAt != null ? createdAt.toString() : "";
        
        return userIdStr + "_" + productIdStr + "_" + createdAtStr;
    }
    
    private String buildReceiptKey(@NonNull Long userId, @NonNull Long productId) {
        return userId + "_" + productId;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseReportDTO generateReport(String query, Map<String, List<String>> filterParams) {
        SearchFilters filters = extractFilters(filterParams);
        
        ClientData clientData = fetchClientData(query, filters.clientFilterParams(), filters.clientTypeId());
        List<Long> sourceIds = resolveSourceIds(query, filters.clientFilterParams(), filters.clientTypeId());
        
        Specification<Purchase> spec = new PurchaseSpecification(query, filters.purchaseFilterParams(), 
                clientData.clientIds(), sourceIds);
        List<Purchase> purchases = purchaseRepository.findAll(spec);
        
        if (purchases.isEmpty()) {
            return new PurchaseReportDTO();
        }
        
        Set<Long> userIds = purchases.stream()
                .map(Purchase::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> productIds = purchases.stream()
                .map(Purchase::getProduct)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> sourceIdSet = purchases.stream()
                .map(Purchase::getSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, String> userNamesMap = loadUserNames(userIds);
        Map<Long, String> productNamesMap = loadProductNames(productIds);
        Map<Long, String> sourceNamesMap = loadSourceNames(sourceIdSet);
        
        List<PurchaseReportDTO.DriverReport> driverReports = buildDriverReports(purchases, userNamesMap, productNamesMap);
        List<PurchaseReportDTO.SourceReport> sourceReports = buildSourceReports(purchases, sourceNamesMap, productNamesMap);
        List<PurchaseReportDTO.ProductTotal> totals = buildProductTotals(purchases, productNamesMap);
        
        PurchaseReportDTO report = new PurchaseReportDTO();
        report.setDrivers(driverReports);
        report.setSources(sourceReports);
        report.setTotals(totals);
        
        return report;
    }
    
    private Map<Long, String> loadUserNames(@NonNull Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<UserDTO> users = userService.getAllUsers();
        return users.stream()
                .filter(user -> user != null && user.getId() != null && userIds.contains(user.getId()))
                .collect(Collectors.toMap(
                        UserDTO::getId,
                        user -> user.getName() != null ? user.getName() : "Невідомий",
                        (existing, _) -> existing
                ));
    }
    
    private Map<Long, String> loadProductNames(@NonNull Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Iterable<Product> products = productRepository.findAllById(productIds);
        return StreamSupport.stream(products.spliterator(), false)
                .filter(product -> product != null && product.getId() != null)
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product.getName() != null ? product.getName() : "Невідомий",
                        (existing, _) -> existing
                ));
    }
    
    private Map<Long, String> loadSourceNames(@NonNull Set<Long> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, String> sourceNamesMap = new HashMap<>();
        for (Long sourceId : sourceIds) {
            try {
                SourceDTO sourceDTO = sourceService.getSourceName(sourceId);
                if (sourceDTO != null && sourceDTO.getName() != null) {
                    sourceNamesMap.put(sourceId, sourceDTO.getName());
                } else {
                    sourceNamesMap.put(sourceId, "Невідомий");
                }
            } catch (Exception e) {
                log.warn("Failed to load source name for sourceId={}: {}", sourceId, e.getMessage());
                sourceNamesMap.put(sourceId, "Невідомий");
            }
        }
        return sourceNamesMap;
    }
    
    private List<PurchaseReportDTO.DriverReport> buildDriverReports(@NonNull List<Purchase> purchases,
                                                                     @NonNull Map<Long, String> userNamesMap,
                                                                     @NonNull Map<Long, String> productNamesMap) {
        Map<Long, Map<Long, PurchaseReportDTO.ProductInfo>> driverProductMap = new HashMap<>();
        
        for (Purchase purchase : purchases) {
            Long userId = purchase.getUser();
            Long productId = purchase.getProduct();
            
            if (userId == null || productId == null) {
                continue;
            }
            
            driverProductMap.computeIfAbsent(userId, _ -> new HashMap<>());
            Map<Long, PurchaseReportDTO.ProductInfo> productMap = driverProductMap.get(userId);
            
            productMap.computeIfAbsent(productId, _ -> {
                PurchaseReportDTO.ProductInfo info = new PurchaseReportDTO.ProductInfo();
                info.setProductId(productId);
                info.setProductName(productNamesMap.getOrDefault(productId, "Невідомий"));
                info.setQuantity(BigDecimal.ZERO);
                info.setTotalPriceEur(BigDecimal.ZERO);
                return info;
            });
            
            PurchaseReportDTO.ProductInfo info = productMap.get(productId);
            if (purchase.getQuantity() != null) {
                info.setQuantity(info.getQuantity().add(purchase.getQuantity()));
            }
            if (purchase.getTotalPriceEur() != null) {
                info.setTotalPriceEur(info.getTotalPriceEur().add(purchase.getTotalPriceEur()));
            }
        }
        
        return driverProductMap.entrySet().stream()
                .map(entry -> {
                    PurchaseReportDTO.DriverReport report = new PurchaseReportDTO.DriverReport();
                    report.setUserId(entry.getKey());
                    report.setUserName(userNamesMap.getOrDefault(entry.getKey(), "Невідомий"));
                    report.setProducts(new ArrayList<>(entry.getValue().values()));
                    return report;
                })
                .sorted(Comparator.comparing(PurchaseReportDTO.DriverReport::getUserName))
                .collect(Collectors.toList());
    }
    
    private List<PurchaseReportDTO.SourceReport> buildSourceReports(@NonNull List<Purchase> purchases,
                                                                    @NonNull Map<Long, String> sourceNamesMap,
                                                                    @NonNull Map<Long, String> productNamesMap) {
        Map<Long, Map<Long, PurchaseReportDTO.ProductInfo>> sourceProductMap = new HashMap<>();
        
        for (Purchase purchase : purchases) {
            Long sourceId = purchase.getSource();
            Long productId = purchase.getProduct();
            
            if (sourceId == null || productId == null) {
                continue;
            }
            
            sourceProductMap.computeIfAbsent(sourceId, _ -> new HashMap<>());
            Map<Long, PurchaseReportDTO.ProductInfo> productMap = sourceProductMap.get(sourceId);
            
            productMap.computeIfAbsent(productId, _ -> {
                PurchaseReportDTO.ProductInfo info = new PurchaseReportDTO.ProductInfo();
                info.setProductId(productId);
                info.setProductName(productNamesMap.getOrDefault(productId, "Невідомий"));
                info.setQuantity(BigDecimal.ZERO);
                info.setTotalPriceEur(BigDecimal.ZERO);
                return info;
            });
            
            PurchaseReportDTO.ProductInfo info = productMap.get(productId);
            if (purchase.getQuantity() != null) {
                info.setQuantity(info.getQuantity().add(purchase.getQuantity()));
            }
            if (purchase.getTotalPriceEur() != null) {
                info.setTotalPriceEur(info.getTotalPriceEur().add(purchase.getTotalPriceEur()));
            }
        }
        
        return sourceProductMap.entrySet().stream()
                .map(entry -> {
                    PurchaseReportDTO.SourceReport report = new PurchaseReportDTO.SourceReport();
                    report.setSourceId(entry.getKey());
                    report.setSourceName(sourceNamesMap.getOrDefault(entry.getKey(), "Невідомий"));
                    report.setProducts(new ArrayList<>(entry.getValue().values()));
                    return report;
                })
                .sorted(Comparator.comparing(PurchaseReportDTO.SourceReport::getSourceName))
                .collect(Collectors.toList());
    }
    
    private List<PurchaseReportDTO.ProductTotal> buildProductTotals(@NonNull List<Purchase> purchases,
                                                                    @NonNull Map<Long, String> productNamesMap) {
        Map<Long, PurchaseReportDTO.ProductTotal> productTotalMap = new HashMap<>();
        
        for (Purchase purchase : purchases) {
            Long productId = purchase.getProduct();
            
            if (productId == null) {
                continue;
            }
            
            productTotalMap.computeIfAbsent(productId, _ -> {
                PurchaseReportDTO.ProductTotal total = new PurchaseReportDTO.ProductTotal();
                total.setProductId(productId);
                total.setProductName(productNamesMap.getOrDefault(productId, "Невідомий"));
                total.setQuantity(BigDecimal.ZERO);
                total.setTotalPriceEur(BigDecimal.ZERO);
                return total;
            });
            
            PurchaseReportDTO.ProductTotal total = productTotalMap.get(productId);
            if (purchase.getQuantity() != null) {
                total.setQuantity(total.getQuantity().add(purchase.getQuantity()));
            }
            if (purchase.getTotalPriceEur() != null) {
                total.setTotalPriceEur(total.getTotalPriceEur().add(purchase.getTotalPriceEur()));
            }
        }
        
        return productTotalMap.values().stream()
                .sorted(Comparator.comparing(PurchaseReportDTO.ProductTotal::getProductName))
                .collect(Collectors.toList());
    }

}
