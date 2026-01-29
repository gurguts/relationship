package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.Purchase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseSearchPredicateBuilder {
    
    private static final String FIELD_ID = "id";
    private static final String FIELD_COMMENT = "comment";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_CLIENT = "client";
    
    private final PurchaseFilterValueParser valueParser;
    
    public List<Predicate> buildSearchPredicates(
            @NonNull String query,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull List<Long> sourceIds,
            @NonNull List<Long> clientIds) {
        
        List<Predicate> searchPredicates = new ArrayList<>();
        
        addIdSearchPredicate(searchPredicates, query, root, criteriaBuilder);
        addCommentSearchPredicate(searchPredicates, query, root, criteriaBuilder);
        
        if (!sourceIds.isEmpty()) {
            searchPredicates.add(root.get(FIELD_SOURCE).in(sourceIds));
        }
        
        if (!clientIds.isEmpty()) {
            searchPredicates.add(root.get(FIELD_CLIENT).in(clientIds));
        }
        
        return searchPredicates;
    }
    
    private void addIdSearchPredicate(
            @NonNull List<Predicate> searchPredicates,
            @NonNull String query,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder) {
        
        Long idValue = valueParser.tryParseLong(query);
        if (idValue != null) {
            searchPredicates.add(criteriaBuilder.equal(root.get(FIELD_ID), idValue));
        } else {
            searchPredicates.add(
                    criteriaBuilder.like(criteriaBuilder.toString(root.get(FIELD_ID)),
                            String.format("%%%s%%", query.toLowerCase()))
            );
        }
    }
    
    private void addCommentSearchPredicate(
            @NonNull List<Predicate> searchPredicates,
            @NonNull String query,
            @NonNull Root<Purchase> root,
            @NonNull CriteriaBuilder criteriaBuilder) {
        
        searchPredicates.add(
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get(FIELD_COMMENT)),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(FIELD_COMMENT)),
                                String.format("%%%s%%", query.toLowerCase()))
                )
        );
    }
}
