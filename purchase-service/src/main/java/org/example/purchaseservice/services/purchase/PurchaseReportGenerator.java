package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.services.impl.ISourceService;
import org.example.purchaseservice.services.impl.IUserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PurchaseReportGenerator {
    
    private final IUserService userService;
    private final ProductRepository productRepository;
    private final ISourceService sourceService;
    
    public PurchaseReportDTO generateReport(@NonNull List<Purchase> purchases) {
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
            processPurchaseForMap(driverProductMap, purchase, Purchase::getUser, productNamesMap);
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
            processPurchaseForMap(sourceProductMap, purchase, Purchase::getSource, productNamesMap);
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
    
    private void processPurchaseForMap(@NonNull Map<Long, Map<Long, PurchaseReportDTO.ProductInfo>> entityProductMap,
                                       @NonNull Purchase purchase,
                                       @NonNull java.util.function.Function<Purchase, Long> keyExtractor,
                                       @NonNull Map<Long, String> productNamesMap) {
        Long entityId = keyExtractor.apply(purchase);
        Long productId = purchase.getProduct();
        
        if (entityId == null || productId == null) {
            return;
        }
        
        entityProductMap.computeIfAbsent(entityId, _ -> new HashMap<>());
        Map<Long, PurchaseReportDTO.ProductInfo> productMap = entityProductMap.get(entityId);
        updateProductInfo(productMap, productId, purchase, productNamesMap);
    }
    
    private void updateProductInfo(@NonNull Map<Long, PurchaseReportDTO.ProductInfo> productMap,
                                   @NonNull Long productId,
                                   @NonNull Purchase purchase,
                                   @NonNull Map<Long, String> productNamesMap) {
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
