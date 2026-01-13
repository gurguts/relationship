package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class VehicleSpecification implements Specification<Vehicle> {
    
    private static final String FILTER_SHIPMENT_DATE_FROM = "shipmentDateFrom";
    private static final String FILTER_SHIPMENT_DATE_TO = "shipmentDateTo";
    private static final String FILTER_CUSTOMS_DATE_FROM = "customsDateFrom";
    private static final String FILTER_CUSTOMS_DATE_TO = "customsDateTo";
    private static final String FILTER_CUSTOMS_CLEARANCE_DATE_FROM = "customsClearanceDateFrom";
    private static final String FILTER_CUSTOMS_CLEARANCE_DATE_TO = "customsClearanceDateTo";
    private static final String FILTER_UNLOADING_DATE_FROM = "unloadingDateFrom";
    private static final String FILTER_UNLOADING_DATE_TO = "unloadingDateTo";
    private static final String FILTER_IS_OUR_VEHICLE = "isOurVehicle";
    
    private static final String FIELD_ID = "id";
    private static final String FIELD_SHIPMENT_DATE = "shipmentDate";
    private static final String FIELD_CUSTOMS_DATE = "customsDate";
    private static final String FIELD_CUSTOMS_CLEARANCE_DATE = "customsClearanceDate";
    private static final String FIELD_UNLOADING_DATE = "unloadingDate";
    private static final String FIELD_IS_OUR_VEHICLE = "isOurVehicle";
    private static final String FIELD_VEHICLE_NUMBER = "vehicleNumber";
    private static final String FIELD_INVOICE_UA = "invoiceUa";
    private static final String FIELD_INVOICE_EU = "invoiceEu";
    private static final String FIELD_PRODUCT = "product";
    private static final String FIELD_PRODUCT_QUANTITY = "productQuantity";
    private static final String FIELD_DECLARATION_NUMBER = "declarationNumber";
    private static final String FIELD_DRIVER_FULL_NAME = "driverFullName";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_RECEIVER = "receiver";
    private static final String FIELD_CARRIER = "carrier";
    private static final String FIELD_TERMINAL = "terminal";
    private static final String FIELD_DESTINATION_COUNTRY = "destinationCountry";
    private static final String FIELD_DESTINATION_PLACE = "destinationPlace";
    private static final String FIELD_SENDER_NAME = "name";
    private static final String FIELD_RECEIVER_NAME = "name";
    private static final String FIELD_CARRIER_COMPANY_NAME = "companyName";
    private static final String FIELD_TERMINAL_NAME = "name";
    private static final String FIELD_DESTINATION_COUNTRY_NAME = "name";
    private static final String FIELD_DESTINATION_PLACE_NAME = "name";
    
    private final String query;
    private final Map<String, List<String>> filterParams;

    public VehicleSpecification(String query, Map<String, List<String>> filterParams) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Collections.emptyMap();
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Vehicle> root, CriteriaQuery<?> criteriaQuery,
                                 @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = applyFilters(criteriaBuilder.conjunction(), root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (StringUtils.hasText(this.query)) {
            List<Predicate> searchPredicates = buildSearchPredicates(root, criteriaBuilder);
            Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
            predicates.add(searchPredicate);
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private List<Predicate> buildSearchPredicates(@NonNull Root<Vehicle> root, @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> searchPredicates = new ArrayList<>();
        String searchTerm = String.format("%%%s%%", this.query.toLowerCase());

        addIdSearchPredicate(searchPredicates, root, criteriaBuilder, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_VEHICLE_NUMBER, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_INVOICE_UA, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_INVOICE_EU, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_PRODUCT, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_PRODUCT_QUANTITY, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DECLARATION_NUMBER, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DRIVER_FULL_NAME, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DESCRIPTION, searchTerm);

        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_SENDER, FIELD_SENDER_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_RECEIVER, FIELD_RECEIVER_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_CARRIER, FIELD_CARRIER_COMPANY_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_TERMINAL, FIELD_TERMINAL_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DESTINATION_COUNTRY, FIELD_DESTINATION_COUNTRY_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DESTINATION_PLACE, FIELD_DESTINATION_PLACE_NAME, searchTerm);

        return searchPredicates;
    }

    private void addIdSearchPredicate(@NonNull List<Predicate> searchPredicates,
                                      @NonNull Root<Vehicle> root,
                                      @NonNull CriteriaBuilder criteriaBuilder,
                                      @NonNull String searchTerm) {
        Long idValue = tryParseLong(this.query);
        if (idValue != null) {
            searchPredicates.add(criteriaBuilder.equal(root.get(FIELD_ID), idValue));
        } else {
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.toString(root.get(FIELD_ID))), searchTerm));
        }
    }

    private void addStringFieldSearchPredicate(@NonNull List<Predicate> searchPredicates,
                                                @NonNull Root<Vehicle> root,
                                                @NonNull CriteriaBuilder criteriaBuilder,
                                                @NonNull String fieldName,
                                                @NonNull String searchTerm) {
        searchPredicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), searchTerm)
        );
    }

    private void addJoinFieldSearchPredicate(@NonNull List<Predicate> searchPredicates,
                                             @NonNull Root<Vehicle> root,
                                             @NonNull CriteriaBuilder criteriaBuilder,
                                             @NonNull String joinField,
                                             @NonNull String joinProperty,
                                             @NonNull String searchTerm) {
        Join<Vehicle, ?> join = root.join(joinField, JoinType.LEFT);
        searchPredicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(join.get(joinProperty)), searchTerm)
        );
    }

    private Long tryParseLong(@NonNull String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Predicate applyFilters(Predicate predicate, @NonNull Root<Vehicle> root, @NonNull CriteriaBuilder criteriaBuilder) {
        if (filterParams.isEmpty()) {
            return predicate;
        }
        
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (key == null || values == null || values.isEmpty()) {
                continue;
            }

            try {
                predicate = applyFilter(predicate, root, criteriaBuilder, key, values);
            } catch (Exception e) {
                log.error("Error applying filter: key={}, values={}", key, values, e);
            }
        }

        return predicate;
    }

    private Predicate applyFilter(Predicate predicate, @NonNull Root<Vehicle> root,
                                   @NonNull CriteriaBuilder criteriaBuilder,
                                   @NonNull String key, @NonNull List<String> values) {
        return switch (key) {
            case FILTER_SHIPMENT_DATE_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_SHIPMENT_DATE, true);
            case FILTER_SHIPMENT_DATE_TO -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_SHIPMENT_DATE, false);
            case FILTER_CUSTOMS_DATE_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_CUSTOMS_DATE, true);
            case FILTER_CUSTOMS_DATE_TO -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_CUSTOMS_DATE, false);
            case FILTER_CUSTOMS_CLEARANCE_DATE_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_CUSTOMS_CLEARANCE_DATE, true);
            case FILTER_CUSTOMS_CLEARANCE_DATE_TO -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_CUSTOMS_CLEARANCE_DATE, false);
            case FILTER_UNLOADING_DATE_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_UNLOADING_DATE, true);
            case FILTER_UNLOADING_DATE_TO -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_UNLOADING_DATE, false);
            case FILTER_IS_OUR_VEHICLE -> addBooleanFilter(predicate, root, criteriaBuilder, values);
            default -> predicate;
        };
    }

    private Predicate addDateFilter(Predicate predicate, @NonNull Root<Vehicle> root,
                                    @NonNull CriteriaBuilder criteriaBuilder,
                                    @NonNull List<String> values, @NonNull String field, boolean isFrom) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        try {
            String dateString = values.getFirst();
            if (dateString == null || dateString.trim().isEmpty()) {
                return predicate;
            }
            
            LocalDate date = LocalDate.parse(dateString.trim());
            LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), dateTime) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), dateTime));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date filter: field={}, value={}", field, values.getFirst(), e);
        } catch (Exception e) {
            log.error("Error adding date filter: field={}, values={}", field, values, e);
        }
        
        return predicate;
    }

    private Predicate addBooleanFilter(Predicate predicate, @NonNull Root<Vehicle> root,
                                       @NonNull CriteriaBuilder criteriaBuilder,
                                       @NonNull List<String> values) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        try {
            String valueString = values.getFirst();
            if (valueString == null || valueString.trim().isEmpty()) {
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

