package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseWithdrawalFilterPredicateFactory {
    
    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_WAREHOUSE_ID = "warehouseId";
    public static final String FIELD_WITHDRAWAL_REASON = "withdrawalReason";
    public static final String FIELD_WITHDRAWAL_REASON_ID = "id";
    public static final String FIELD_WITHDRAWAL_DATE = "withdrawalDate";
    
    private final WarehouseWithdrawalFilterValueParser valueParser;
    private final AbstractFilterPredicateFactory abstractFactory;
    
    public Predicate addIdFilter(
            @NonNull Root<WarehouseWithdrawal> root,
            @NonNull String fieldName,
            @NonNull List<String> values) {
        
        return abstractFactory.parseAndCreateIdPredicate(
                values,
                valueParser::parseLong,
                root,
                fieldName,
                "Error parsing ID filter");
    }
    
    public Predicate addWithdrawalReasonIdFilter(
            @NonNull Root<WarehouseWithdrawal> root,
            @NonNull List<String> values) {
        
        return abstractFactory.parseAndCreateNestedIdPredicate(
                values,
                valueParser::parseLong,
                root,
                FIELD_WITHDRAWAL_REASON,
                FIELD_WITHDRAWAL_REASON_ID,
                "Error parsing withdrawal reason ID filter");
    }
    
    public Predicate addDateFilter(
            @NonNull Root<WarehouseWithdrawal> root,
            @NonNull CriteriaBuilder cb,
            @NonNull List<String> values,
            boolean isFrom) {
        
        return abstractFactory.addDateFilterWithField(root, cb, values, FIELD_WITHDRAWAL_DATE, isFrom);
    }
}
