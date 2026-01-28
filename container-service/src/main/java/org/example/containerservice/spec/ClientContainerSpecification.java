package org.example.containerservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.containerservice.models.ClientContainer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.example.containerservice.spec.ClientContainerFilterBuilder.*;
import static org.example.containerservice.spec.ClientContainerFilterValueParser.escapeQuery;

public class ClientContainerSpecification implements Specification<ClientContainer> {

    private static final String LIKE_PATTERN = "%%%s%%";

    private static final String FIELD_ID = "id";
    private static final String FIELD_CLIENT = "client";

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
                case FILTER_UPDATED_AT_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, true, FILTER_UPDATED_AT_FROM);
                case FILTER_UPDATED_AT_TO -> addDateFilter(predicate, root, criteriaBuilder, values, false, FILTER_UPDATED_AT_TO);
                case FILTER_QUANTITY_FROM -> addNumericFilter(predicate, root, criteriaBuilder, values, true, FILTER_QUANTITY_FROM);
                case FILTER_QUANTITY_TO -> addNumericFilter(predicate, root, criteriaBuilder, values, false, FILTER_QUANTITY_TO);
                case FILTER_CONTAINER -> addIdContainerFilter(predicate, root, criteriaBuilder, values, FILTER_CONTAINER);
                case FILTER_USER -> addIdFilter(predicate, root, criteriaBuilder, values, FILTER_USER);
                default -> predicate;
            };
        }

        return predicate;
    }

}