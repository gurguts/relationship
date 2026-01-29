package org.example.userservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public class TransactionSpecification implements Specification<Transaction> {

    private final Map<String, List<String>> filters;

    public TransactionSpecification(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Transaction> root, CriteriaQuery<?> query, @NonNull CriteriaBuilder cb) {
        if (filters == null || filters.isEmpty()) {
            return cb.conjunction();
        }

        TransactionFilterValueParser parser = new TransactionFilterValueParser(filters);
        TransactionFilterPredicateFactory factory = new TransactionFilterPredicateFactory(parser);
        List<Predicate> predicates = factory.buildPredicates(root, cb);

        return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
    }
}
