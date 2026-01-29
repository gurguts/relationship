package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WarehouseReceiptFilterBuilder {
    
    private static final String FILTER_USER_ID = "user_id";
    private static final String FILTER_PRODUCT_ID = "product_id";
    private static final String FILTER_WAREHOUSE_ID = "warehouse_id";
    private static final String FILTER_TYPE = "type";
    private static final String FILTER_ENTRY_DATE_FROM = "entry_date_from";
    private static final String FILTER_ENTRY_DATE_TO = "entry_date_to";
    
    private final WarehouseReceiptFilterPredicateFactory predicateFactory;
    private final AbstractFilterBuilderHelper helper;
    
    public List<Predicate> buildPredicates(
            @NonNull Map<String, List<String>> filters,
            @NonNull Root<WarehouseReceipt> root,
            @NonNull CriteriaBuilder cb) {
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (filters.isEmpty()) {
            return predicates;
        }
        
        applyIdFilters(predicates, filters, root);
        applyDateFilters(predicates, filters, root, cb);
        
        return predicates;
    }
    
    private void applyIdFilters(
            @NonNull List<Predicate> predicates,
            @NonNull Map<String, List<String>> filters,
            @NonNull Root<WarehouseReceipt> root) {
        
        List<String> userIds = filters.get(FILTER_USER_ID);
        if (userIds != null) {
            helper.addPredicateIfNotNull(predicates, () -> predicateFactory.addIdFilter(root,
                    WarehouseReceiptFilterPredicateFactory.FIELD_USER_ID, userIds));
        }
        List<String> productIds = filters.get(FILTER_PRODUCT_ID);
        if (productIds != null) {
            helper.addPredicateIfNotNull(predicates, () -> predicateFactory.addIdFilter(root,
                    WarehouseReceiptFilterPredicateFactory.FIELD_PRODUCT_ID, productIds));
        }
        List<String> warehouseIds = filters.get(FILTER_WAREHOUSE_ID);
        if (warehouseIds != null) {
            helper.addPredicateIfNotNull(predicates, () -> predicateFactory.addIdFilter(root,
                    WarehouseReceiptFilterPredicateFactory.FIELD_WAREHOUSE_ID, warehouseIds));
        }
        List<String> typeIds = filters.get(FILTER_TYPE);
        if (typeIds != null) {
            helper.addPredicateIfNotNull(predicates, () -> predicateFactory.addTypeIdFilter(root, typeIds));
        }
    }
    
    private void applyDateFilters(
            @NonNull List<Predicate> predicates,
            @NonNull Map<String, List<String>> filters,
            @NonNull Root<WarehouseReceipt> root,
            @NonNull CriteriaBuilder cb) {
        
        List<String> dateFrom = filters.get(FILTER_ENTRY_DATE_FROM);
        if (dateFrom != null) {
            helper.addPredicateIfNotNull(predicates, () -> predicateFactory.addDateFilter(root, cb, dateFrom, true));
        }
        List<String> dateTo = filters.get(FILTER_ENTRY_DATE_TO);
        if (dateTo != null) {
            helper.addPredicateIfNotNull(predicates, () -> predicateFactory.addDateFilter(root, cb, dateTo, false));
        }
    }
}
