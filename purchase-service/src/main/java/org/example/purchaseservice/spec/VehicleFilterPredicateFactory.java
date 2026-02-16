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
    private static final String FIELD_MANAGER_ID = "managerId";
    
    private final AbstractFilterPredicateFactory abstractFactory;
    
    public Predicate addManagerIdFilter(
            Predicate predicate,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<String> values) {
        if (abstractFactory.isEmpty(values)) {
            return predicate;
        }
        Predicate idPredicate = abstractFactory.parseAndCreateIdPredicate(
                values, Long::parseLong, root, FIELD_MANAGER_ID, "Error parsing managerId filter");
        if (idPredicate != null) {
            predicate = criteriaBuilder.and(predicate, idPredicate);
        }
        return predicate;
    }
    
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
}
