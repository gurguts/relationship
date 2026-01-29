package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WarehouseWithdrawalSpecification implements Specification<WarehouseWithdrawal> {
    
    private final Map<String, List<String>> filters;
    private final WarehouseWithdrawalFilterBuilder filterBuilder;

    public WarehouseWithdrawalSpecification(
            Map<String, List<String>> filters,
            WarehouseWithdrawalFilterBuilder filterBuilder) {
        this.filters = filters != null ? filters : Collections.emptyMap();
        this.filterBuilder = filterBuilder;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<WarehouseWithdrawal> root, CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = filterBuilder.buildPredicates(filters, root, cb);
        return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
    }
}