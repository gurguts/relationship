package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleFilterPredicateFactory {
    
    public static final String FIELD_SHIPMENT_DATE = "shipmentDate";
    public static final String FIELD_CUSTOMS_DATE = "customsDate";
    public static final String FIELD_CUSTOMS_CLEARANCE_DATE = "customsClearanceDate";
    public static final String FIELD_UNLOADING_DATE = "unloadingDate";
    public static final String FIELD_IS_OUR_VEHICLE = "isOurVehicle";
    
    private final AbstractFilterPredicateFactory abstractFactory;
    
    public Predicate addDateFilter(
            Predicate predicate,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values,
            @NonNull String field,
            boolean isFrom) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        Predicate datePredicate = abstractFactory.addDateFilterWithField(root, criteriaBuilder, values, field, isFrom);
        if (datePredicate != null) {
            predicate = criteriaBuilder.and(predicate, datePredicate);
        }
        
        return predicate;
    }
    
    public Predicate addBooleanFilter(
            Predicate predicate,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values) {
        
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        
        try {
            String valueString = abstractFactory.getFirstNonEmptyValue(values);
            if (valueString == null) {
                return predicate;
            }
            
            String trimmedValue = valueString.trim();
            if ("true".equalsIgnoreCase(trimmedValue)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(root.get(FIELD_IS_OUR_VEHICLE)));
            } else if ("false".equalsIgnoreCase(trimmedValue)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.isFalse(root.get(FIELD_IS_OUR_VEHICLE)));
            }
        } catch (Exception e) {
            log.error("Error adding boolean filter: field={}, values={}", FIELD_IS_OUR_VEHICLE, values, e);
        }
        
        return predicate;
    }
}
