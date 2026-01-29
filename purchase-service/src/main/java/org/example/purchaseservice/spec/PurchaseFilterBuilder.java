package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.Purchase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PurchaseFilterBuilder {
    
    private static final String FILTER_CREATED_AT_FROM = "createdAtFrom";
    private static final String FILTER_CREATED_AT_TO = "createdAtTo";
    private static final String FILTER_QUANTITY_FROM = "quantityFrom";
    private static final String FILTER_QUANTITY_TO = "quantityTo";
    private static final String FILTER_UNIT_PRICE_FROM = "unitPriceFrom";
    private static final String FILTER_UNIT_PRICE_TO = "unitPriceTo";
    private static final String FILTER_TOTAL_PRICE_FROM = "totalPriceFrom";
    private static final String FILTER_TOTAL_PRICE_TO = "totalPriceTo";
    private static final String FILTER_SOURCE = "source";
    private static final String FILTER_PRODUCT = "product";
    private static final String FILTER_USER = "user";
    private static final String FILTER_CURRENCY = "currency";
    private static final String FILTER_CURRENCY_TYPE = "currencyType";
    private static final String FILTER_PAYMENT_METHOD = "paymentMethod";
    private static final String SUFFIX_FROM = "From";
    private static final String SUFFIX_TO = "To";
    
    private final PurchaseFilterPredicateFactory predicateFactory;
    private final AbstractFilterBuilder abstractFilterBuilder;
    
    public Predicate applyFilters(
            Predicate predicate,
            @NonNull Map<String, List<String>> filterParams,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder) {
        
        return abstractFilterBuilder.applyFilters(
                predicate,
                filterParams,
                (pred, entry) -> applyFilter(pred, root, criteriaBuilder, entry.getKey(), entry.getValue())
        );
    }
    
    private Predicate applyFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull String key,
            @NonNull List<String> values) {
        
        return switch (key) {
            case FILTER_CREATED_AT_FROM -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values, true);
            case FILTER_CREATED_AT_TO -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values, false);
            case FILTER_QUANTITY_FROM -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_QUANTITY, true);
            case FILTER_QUANTITY_TO -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_QUANTITY, false);
            case FILTER_UNIT_PRICE_FROM -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_UNIT_PRICE, true);
            case FILTER_UNIT_PRICE_TO -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_UNIT_PRICE, false);
            case FILTER_TOTAL_PRICE_FROM -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_TOTAL_PRICE, true);
            case FILTER_TOTAL_PRICE_TO -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_TOTAL_PRICE, false);
            case FILTER_SOURCE -> predicateFactory.addIdFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_SOURCE);
            case FILTER_PRODUCT -> predicateFactory.addIdFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_PRODUCT);
            case FILTER_USER -> predicateFactory.addIdFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_USER);
            case FILTER_CURRENCY, FILTER_CURRENCY_TYPE -> predicateFactory.addStringFilter(predicate, root, criteriaBuilder, values);
            case FILTER_PAYMENT_METHOD -> predicateFactory.addPaymentMethodFilter(predicate, root, criteriaBuilder, values);
            default -> handleDynamicFilter(predicate, root, criteriaBuilder, key, values);
        };
    }
    
    private Predicate handleDynamicFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull String key,
            @NonNull List<String> values) {
        
        if (key.endsWith(SUFFIX_FROM)) {
            String fieldName = key.substring(0, key.length() - SUFFIX_FROM.length());
            return applyRangeFilter(predicate, root, criteriaBuilder, values, fieldName, true);
        } else if (key.endsWith(SUFFIX_TO)) {
            String fieldName = key.substring(0, key.length() - SUFFIX_TO.length());
            return applyRangeFilter(predicate, root, criteriaBuilder, values, fieldName, false);
        }
        return predicate;
    }
    
    private Predicate applyRangeFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values,
            @NonNull String fieldName,
            boolean isFrom) {
        
        return switch (fieldName) {
            case PurchaseFilterPredicateFactory.FIELD_CREATED_AT -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values, isFrom);
            case PurchaseFilterPredicateFactory.FIELD_QUANTITY -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_QUANTITY, isFrom);
            case PurchaseFilterPredicateFactory.FIELD_UNIT_PRICE -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_UNIT_PRICE, isFrom);
            case PurchaseFilterPredicateFactory.FIELD_TOTAL_PRICE -> predicateFactory.addNumericFilter(predicate, root, criteriaBuilder, values,
                    PurchaseFilterPredicateFactory.FIELD_TOTAL_PRICE, isFrom);
            default -> predicate;
        };
    }
}
