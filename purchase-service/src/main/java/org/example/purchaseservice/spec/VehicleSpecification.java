package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VehicleSpecification implements Specification<Vehicle> {
    
    private final String query;
    private final Map<String, List<String>> filterParams;
    private final String sortField;
    private final org.springframework.data.domain.Sort.Direction sortDirection;
    
    private final VehicleFilterBuilder filterBuilder;
    private final VehicleSearchPredicateBuilder searchPredicateBuilder;

    public VehicleSpecification(
            String query,
            Map<String, List<String>> filterParams,
            VehicleFilterBuilder filterBuilder,
            VehicleSearchPredicateBuilder searchPredicateBuilder) {
        this(query, filterParams, filterBuilder, searchPredicateBuilder, null, null);
    }

    public VehicleSpecification(
            String query,
            Map<String, List<String>> filterParams,
            VehicleFilterBuilder filterBuilder,
            VehicleSearchPredicateBuilder searchPredicateBuilder,
            String sortField,
            org.springframework.data.domain.Sort.Direction sortDirection) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Collections.emptyMap();
        this.filterBuilder = filterBuilder;
        this.searchPredicateBuilder = searchPredicateBuilder;
        this.sortField = sortField;
        this.sortDirection = sortDirection;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Vehicle> root, CriteriaQuery<?> criteriaQuery,
                                 @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = filterBuilder.applyFilters(
                criteriaBuilder.conjunction(), filterParams, root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (StringUtils.hasText(this.query)) {
            List<Predicate> searchPredicates = searchPredicateBuilder.buildSearchPredicates(
                    this.query, root, criteriaBuilder);
            Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
            predicates.add(searchPredicate);
        }

        if (sortField != null
                && sortDirection != null
                && criteriaQuery.getResultType() != Long.class
                && criteriaQuery.getResultType() != long.class) {
            if ("customsDate".equals(sortField)) {
                var customsDatePath = root.get("customsDate");
                var nullRank = criteriaBuilder.selectCase()
                        .when(criteriaBuilder.isNull(customsDatePath), 0)
                        .otherwise(1);
                var customsDateOrder = sortDirection.isAscending()
                        ? criteriaBuilder.asc(customsDatePath)
                        : criteriaBuilder.desc(customsDatePath);
                criteriaQuery.orderBy(criteriaBuilder.asc(nullRank), customsDateOrder);
            }
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}

