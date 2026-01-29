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
    
    private final VehicleFilterBuilder filterBuilder;
    private final VehicleSearchPredicateBuilder searchPredicateBuilder;

    public VehicleSpecification(
            String query,
            Map<String, List<String>> filterParams,
            VehicleFilterBuilder filterBuilder,
            VehicleSearchPredicateBuilder searchPredicateBuilder) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Collections.emptyMap();
        this.filterBuilder = filterBuilder;
        this.searchPredicateBuilder = searchPredicateBuilder;
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

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}

