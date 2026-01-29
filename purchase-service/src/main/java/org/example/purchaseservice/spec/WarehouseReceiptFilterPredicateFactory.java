package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseReceiptFilterPredicateFactory {
    
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_WAREHOUSE_ID = "warehouseId";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TYPE_ID = "id";
    public static final String FIELD_ENTRY_DATE = "entryDate";
    
    private final WarehouseReceiptFilterValueParser valueParser;
    private final AbstractFilterPredicateFactory abstractFactory;
    
    public Predicate addIdFilter(
            @NonNull Root<WarehouseReceipt> root,
            @NonNull String fieldName,
            @NonNull List<String> values) {
        
        return abstractFactory.parseAndCreateIdPredicate(
                values,
                valueParser::parseLong,
                root,
                fieldName,
                "Error parsing ID filter");
    }
    
    public Predicate addTypeIdFilter(
            @NonNull Root<WarehouseReceipt> root,
            @NonNull List<String> values) {
        
        return abstractFactory.parseAndCreateNestedIdPredicate(
                values,
                valueParser::parseLong,
                root,
                FIELD_TYPE,
                FIELD_TYPE_ID,
                "Error parsing type ID filter");
    }
    
    public Predicate addDateFilter(
            @NonNull Root<WarehouseReceipt> root,
            @NonNull CriteriaBuilder cb,
            @NonNull List<String> values,
            boolean isFrom) {
        
        return abstractFactory.addDateFilterWithField(root, cb, values, FIELD_ENTRY_DATE, isFrom);
    }
}
