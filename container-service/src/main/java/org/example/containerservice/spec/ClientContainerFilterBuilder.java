package org.example.containerservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.containerservice.models.ClientContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.example.containerservice.spec.ClientContainerFilterValueParser.*;

public class ClientContainerFilterBuilder {

    private static final String FIELD_ID = "id";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_CONTAINER = "container";
    private static final String FIELD_USER = "user";

    public static Predicate addIdFilter(Predicate predicate,
                                       Root<ClientContainer> root,
                                       CriteriaBuilder criteriaBuilder,
                                       List<String> values,
                                       String filterName) {
        List<Long> ids = parseIds(values, filterName);
        if (ids.isEmpty()) {
            return predicate;
        }
        return criteriaBuilder.and(predicate, root.get(FIELD_USER).in(ids));
    }

    public static Predicate addIdContainerFilter(Predicate predicate,
                                                 Root<ClientContainer> root,
                                                 CriteriaBuilder criteriaBuilder,
                                                 List<String> values,
                                                 String filterName) {
        List<Long> ids = parseIds(values, filterName);
        if (ids.isEmpty()) {
            return predicate;
        }
        return criteriaBuilder.and(predicate, root.get(FIELD_CONTAINER).get(FIELD_ID).in(ids));
    }

    public static Predicate addDateFilter(Predicate predicate,
                                         Root<ClientContainer> root,
                                         CriteriaBuilder criteriaBuilder,
                                         List<String> values,
                                         boolean isFrom,
                                         String filterName) {
        LocalDateTime dateTime = parseDate(values, isFrom, filterName);
        if (dateTime == null) {
            return predicate;
        }
        Path<LocalDateTime> updatedAtPath = root.get(FIELD_UPDATED_AT);
        Predicate datePredicate = isFrom ?
                criteriaBuilder.greaterThanOrEqualTo(updatedAtPath, dateTime) :
                criteriaBuilder.lessThanOrEqualTo(updatedAtPath, dateTime);
        return criteriaBuilder.and(predicate, datePredicate);
    }

    public static Predicate addNumericFilter(Predicate predicate,
                                            Root<ClientContainer> root,
                                            CriteriaBuilder criteriaBuilder,
                                            List<String> values,
                                            boolean isFrom,
                                            String filterName) {
        Double value = parseNumeric(values, filterName);
        if (value == null) {
            return predicate;
        }
        Path<Double> quantityPath = root.get(FIELD_QUANTITY);
        Predicate numericPredicate = isFrom ?
                criteriaBuilder.greaterThanOrEqualTo(quantityPath, value) :
                criteriaBuilder.lessThanOrEqualTo(quantityPath, value);
        return criteriaBuilder.and(predicate, numericPredicate);
    }
}
