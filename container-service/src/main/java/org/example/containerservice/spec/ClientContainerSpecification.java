package org.example.containerservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.ClientContainer;
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
import java.util.stream.Collectors;

public class ClientContainerSpecification implements Specification<ClientContainer> {

    private static final String ERROR_INVALID_FILTER = "INVALID_FILTER";
    private static final String MESSAGE_INCORRECT_ID_FORMAT = "Incorrect ID format in filter %s: %s";
    private static final String MESSAGE_INCORRECT_DATE_FORMAT = "Incorrect date format in filter %s: %s";
    private static final String MESSAGE_INCORRECT_NUMERIC_FORMAT = "Incorrect numeric format in filter %s: %s";
    private static final String LIKE_PATTERN = "%%%s%%";

    private static final String FIELD_ID = "id";
    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_CONTAINER = "container";
    private static final String FIELD_USER = "user";

    private static final String FILTER_UPDATED_AT_FROM = "updatedAtFrom";
    private static final String FILTER_UPDATED_AT_TO = "updatedAtTo";
    private static final String FILTER_QUANTITY_FROM = "quantityFrom";
    private static final String FILTER_QUANTITY_TO = "quantityTo";
    private static final String FILTER_CONTAINER = "container";
    private static final String FILTER_USER = "user";

    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> clientIds;

    public ClientContainerSpecification(String query, Map<String, List<String>> filterParams,
                                        List<Long> clientIds) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Collections.emptyMap();
        this.clientIds = clientIds;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<ClientContainer> root,
                                 CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = applyFilters(criteriaBuilder.conjunction(), root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (StringUtils.hasText(this.query)) {
            if (clientIds != null && !clientIds.isEmpty()) {
                predicates.add(root.get(FIELD_CLIENT).in(clientIds));
            } else {
                List<Predicate> searchPredicates = buildSearchPredicates(root, criteriaBuilder);
                Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
                predicates.add(searchPredicate);
            }
        } else if (clientIds != null && !clientIds.isEmpty()) {
            predicates.add(root.get(FIELD_CLIENT).in(clientIds));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private List<Predicate> buildSearchPredicates(@NonNull Root<ClientContainer> root, @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> searchPredicates = new ArrayList<>();

        String escapedQuery = escapeQuery(this.query);
        String searchPattern = String.format(LIKE_PATTERN, escapedQuery);
        searchPredicates.add(
                criteriaBuilder.like(
                        criteriaBuilder.toString(root.get(FIELD_ID)), searchPattern)
        );

        return searchPredicates;
    }

    private String escapeQuery(String query) {
        return query.toLowerCase().replace("%", "\\%").replace("_", "\\_");
    }

    private Predicate applyFilters(Predicate predicate,
                                   Root<ClientContainer> root,
                                   CriteriaBuilder criteriaBuilder) {
        if (filterParams == null || filterParams.isEmpty()) {
            return predicate;
        }

        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            predicate = switch (key) {
                case FILTER_UPDATED_AT_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, true);
                case FILTER_UPDATED_AT_TO -> addDateFilter(predicate, root, criteriaBuilder, values, false);
                case FILTER_QUANTITY_FROM -> addNumericFilter(predicate, root, criteriaBuilder, values, true);
                case FILTER_QUANTITY_TO -> addNumericFilter(predicate, root, criteriaBuilder, values, false);
                case FILTER_CONTAINER -> addIdContainerFilter(predicate, root, criteriaBuilder, values);
                case FILTER_USER -> addIdFilter(predicate, root, criteriaBuilder, values);
                default -> predicate;
            };
        }

        return predicate;
    }

    private Predicate addIdFilter(Predicate predicate,
                                  Root<ClientContainer> root,
                                  CriteriaBuilder criteriaBuilder,
                                  List<String> values) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            List<Long> ids = values.stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            predicate = criteriaBuilder.and(predicate, root.get(ClientContainerSpecification.FIELD_USER).in(ids));
        } catch (NumberFormatException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_ID_FORMAT, ClientContainerSpecification.FIELD_USER, values));
        }
        return predicate;
    }

    private Predicate addIdContainerFilter(Predicate predicate,
                                           Root<ClientContainer> root,
                                           CriteriaBuilder criteriaBuilder,
                                           List<String> values) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            List<Long> ids = values.stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            predicate = criteriaBuilder.and(predicate, root.get(FIELD_CONTAINER).get(FIELD_ID).in(ids));
        } catch (NumberFormatException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_ID_FORMAT, FILTER_CONTAINER, values));
        }
        return predicate;
    }

    private Predicate addDateFilter(Predicate predicate,
                                    Root<ClientContainer> root,
                                    CriteriaBuilder criteriaBuilder,
                                    List<String> values,
                                    boolean isFrom) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            LocalDate date = LocalDate.parse(values.getFirst().trim());
            LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(ClientContainerSpecification.FIELD_UPDATED_AT), dateTime) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(ClientContainerSpecification.FIELD_UPDATED_AT), dateTime));
        } catch (DateTimeParseException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_DATE_FORMAT, ClientContainerSpecification.FIELD_UPDATED_AT, values));
        }
        return predicate;
    }

    private Predicate addNumericFilter(Predicate predicate,
                                       Root<ClientContainer> root,
                                       CriteriaBuilder criteriaBuilder,
                                       List<String> values,
                                       boolean isFrom) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            Double value = Double.parseDouble(values.getFirst().trim());
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(ClientContainerSpecification.FIELD_QUANTITY), value) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(ClientContainerSpecification.FIELD_QUANTITY), value));
        } catch (NumberFormatException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_NUMERIC_FORMAT, ClientContainerSpecification.FIELD_QUANTITY, values));
        }
        return predicate;
    }
}