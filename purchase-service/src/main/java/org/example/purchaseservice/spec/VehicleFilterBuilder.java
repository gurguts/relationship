package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VehicleFilterBuilder {
    
    private static final String FILTER_SHIPMENT_DATE_FROM = "shipmentDateFrom";
    private static final String FILTER_SHIPMENT_DATE_TO = "shipmentDateTo";
    private static final String FILTER_CUSTOMS_DATE_FROM = "customsDateFrom";
    private static final String FILTER_CUSTOMS_DATE_TO = "customsDateTo";
    private static final String FILTER_CUSTOMS_CLEARANCE_DATE_FROM = "customsClearanceDateFrom";
    private static final String FILTER_CUSTOMS_CLEARANCE_DATE_TO = "customsClearanceDateTo";
    private static final String FILTER_UNLOADING_DATE_FROM = "unloadingDateFrom";
    private static final String FILTER_UNLOADING_DATE_TO = "unloadingDateTo";
    private static final String FILTER_MANAGER_ID = "managerId";
    
    private final VehicleFilterPredicateFactory predicateFactory;
    private final AbstractFilterBuilder abstractFilterBuilder;
    
    public Predicate applyFilters(
            Predicate predicate,
            @NonNull Map<String, List<String>> filterParams,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder) {
        
        return abstractFilterBuilder.applyFilters(
                predicate,
                filterParams,
                (pred, entry) -> applyFilter(pred, root, criteriaBuilder, entry.getKey(), entry.getValue())
        );
    }
    
    private Predicate applyFilter(
            Predicate predicate,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull String key,
            @NonNull List<String> values) {
        
        return switch (key) {
            case FILTER_SHIPMENT_DATE_FROM -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_SHIPMENT_DATE, true);
            case FILTER_SHIPMENT_DATE_TO -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_SHIPMENT_DATE, false);
            case FILTER_CUSTOMS_DATE_FROM -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_CUSTOMS_DATE, true);
            case FILTER_CUSTOMS_DATE_TO -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_CUSTOMS_DATE, false);
            case FILTER_CUSTOMS_CLEARANCE_DATE_FROM -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_CUSTOMS_CLEARANCE_DATE, true);
            case FILTER_CUSTOMS_CLEARANCE_DATE_TO -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_CUSTOMS_CLEARANCE_DATE, false);
            case FILTER_UNLOADING_DATE_FROM -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_UNLOADING_DATE, true);
            case FILTER_UNLOADING_DATE_TO -> predicateFactory.addDateFilter(predicate, root, criteriaBuilder, values,
                    VehicleFilterPredicateFactory.FIELD_UNLOADING_DATE, false);
            case FILTER_MANAGER_ID -> predicateFactory.addManagerIdFilter(predicate, root, criteriaBuilder, values);
            default -> predicate;
        };
    }
}
