package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
@Component
public class AbstractFilterBuilder {
    
    public Predicate applyFilters(
            Predicate predicate,
            @NonNull Map<String, List<String>> filterParams,
            @NonNull BiFunction<Predicate, Map.Entry<String, List<String>>, Predicate> filterApplier) {
        
        if (filterParams.isEmpty()) {
            return predicate;
        }
        
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            
            if (key == null || values == null || values.isEmpty()) {
                continue;
            }
            
            try {
                predicate = filterApplier.apply(predicate, entry);
            } catch (Exception e) {
                log.error("Error applying filter: key={}, values={}", key, values, e);
            }
        }
        
        return predicate;
    }
}
