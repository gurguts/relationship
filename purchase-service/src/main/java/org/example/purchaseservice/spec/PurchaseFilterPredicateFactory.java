package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseFilterPredicateFactory {
    
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_UNIT_PRICE = "unitPrice";
    public static final String FIELD_TOTAL_PRICE = "totalPrice";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_PRODUCT = "product";
    public static final String FIELD_USER = "user";
    public static final String FIELD_CURRENCY = "currency";
    public static final String FIELD_PAYMENT_METHOD = "paymentMethod";
    
    private final PurchaseFilterValueParser valueParser;
    private final AbstractFilterPredicateFactory abstractFactory;
    
    public Predicate addIdFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values,
            @NonNull String field) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        try {
            List<Long> ids = abstractFactory.parseLongValues(values, valueParser::parseLong);
            
            if (!ids.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, root.get(field).in(ids));
            }
        } catch (Exception e) {
            log.error("Error parsing ID filter: field={}, values={}", field, values, e);
        }
        
        return predicate;
    }
    
    public Predicate addStringFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        List<String> nonNullValues = values.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
        
        if (!nonNullValues.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, root.get(FIELD_CURRENCY).in(nonNullValues));
        }
        
        return predicate;
    }
    
    public Predicate addPaymentMethodFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        List<PaymentMethod> paymentMethods = values.stream()
                .filter(Objects::nonNull)
                .map(valueParser::mapToPaymentMethod)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!paymentMethods.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, root.get(FIELD_PAYMENT_METHOD).in(paymentMethods));
        }
        
        return predicate;
    }
    
    public Predicate addDateFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values,
            boolean isFrom) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        Predicate datePredicate = abstractFactory.addDateFilterWithField(root, criteriaBuilder, values, FIELD_CREATED_AT, isFrom);
        if (datePredicate != null) {
            predicate = criteriaBuilder.and(predicate, datePredicate);
        }
        
        return predicate;
    }
    
    public Predicate addNumericFilter(
            Predicate predicate,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values,
            @NonNull String field,
            boolean isFrom) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        try {
            String valueString = abstractFactory.getFirstNonEmptyValue(values);
            if (valueString == null) {
                return predicate;
            }
            
            Double value = Double.parseDouble(valueString);
            
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), value) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), value));
        } catch (NumberFormatException e) {
            log.error("Error parsing numeric filter: field={}, value={}", field, values, e);
        } catch (Exception e) {
            log.error("Error adding numeric filter: field={}, values={}", field, values, e);
        }
        
        return predicate;
    }

}
