package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.purchaseservice.models.Purchase;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PurchaseSpecification implements Specification<Purchase> {
    
    private static final String FIELD_CLIENT = "client";
    
    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> clientIds;
    private final List<Long> sourceIds;
    
    private final PurchaseFilterBuilder filterBuilder;
    private final PurchaseSearchPredicateBuilder searchPredicateBuilder;

    public PurchaseSpecification(
            String query,
            Map<String, List<String>> filterParams,
            List<Long> clientIds,
            List<Long> sourceIds,
            PurchaseFilterBuilder filterBuilder,
            PurchaseSearchPredicateBuilder searchPredicateBuilder) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Collections.emptyMap();
        this.sourceIds = sourceIds != null ? sourceIds : Collections.emptyList();
        this.clientIds = clientIds != null ? clientIds : Collections.emptyList();
        this.filterBuilder = filterBuilder;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Purchase> root, CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = filterBuilder.applyFilters(
                criteriaBuilder.conjunction(), filterParams, root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (clientIds != null && !clientIds.isEmpty()) {
            predicates.add(root.get(FIELD_CLIENT).in(clientIds));
        } else if (clientIds != null) {
            predicates.add(criteriaBuilder.disjunction());
        } else if (StringUtils.hasText(this.query)) {
            List<Long> safeSourceIds = sourceIds != null ? sourceIds : Collections.emptyList();
            List<Long> safeClientIds = Collections.emptyList();
            List<Predicate> searchPredicates = searchPredicateBuilder.buildSearchPredicates(
                    this.query, root, criteriaBuilder, safeSourceIds, safeClientIds);
            Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
            predicates.add(searchPredicate);
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
