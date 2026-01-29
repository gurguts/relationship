package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class AbstractFilterBuilderHelper {
    
    public void addPredicateIfNotNull(@NonNull List<Predicate> predicates, Supplier<Predicate> predicateSupplier) {
        Predicate predicate = predicateSupplier.get();
        if (predicate != null) {
            predicates.add(predicate);
        }
    }
}
