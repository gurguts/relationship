package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.UserProductIds;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseReceivedStatusBuilder {
    
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    
    public Map<String, Boolean> buildReceivedStatusMap(@NonNull List<Purchase> purchases) {
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
    
    public String buildReceivedStatusKey(@NonNull Purchase purchase) {
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
}
