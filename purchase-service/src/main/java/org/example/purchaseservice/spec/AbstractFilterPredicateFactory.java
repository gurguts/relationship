package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AbstractFilterPredicateFactory {
    
    public boolean isEmpty(@NonNull List<String> values) {
        return values.isEmpty();
    }
    
    public String getFirstNonEmptyValue(@NonNull List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .findFirst()
                .orElse(null);
    }
    
    public LocalDateTime parseDate(@NonNull String dateString, boolean isFrom) {
        LocalDate date = LocalDate.parse(dateString.trim());
        return isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
    }
    
    public <T> Predicate createDatePredicate(
            @NonNull Root<T> root,
            @NonNull CriteriaBuilder cb,
            @NonNull String fieldName,
            @NonNull LocalDateTime dateTime,
            boolean isFrom) {
        return isFrom ?
                cb.greaterThanOrEqualTo(root.get(fieldName), dateTime) :
                cb.lessThanOrEqualTo(root.get(fieldName), dateTime);
    }
    
    public <T> Predicate addDateFilterWithField(
            @NonNull Root<T> root,
            @NonNull CriteriaBuilder cb,
            @NonNull List<String> values,
            @NonNull String fieldName,
            boolean isFrom) {
        
        if (isEmpty(values)) {
            return null;
        }
        
        try {
            String dateString = getFirstNonEmptyValue(values);
            if (dateString == null) {
                return null;
            }
            
            LocalDateTime dateTime = parseDate(dateString, isFrom);
            return createDatePredicate(root, cb, fieldName, dateTime, isFrom);
        } catch (DateTimeParseException e) {
            log.error("Error parsing date filter: fieldName={}, value={}", fieldName, values, e);
        } catch (Exception e) {
            log.error("Error adding date filter: fieldName={}, values={}", fieldName, values, e);
        }
        
        return null;
    }
    
    public <T> List<T> parseLongValues(
            @NonNull List<String> values,
            @NonNull Function<String, T> parser) {
        
        return values.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .map(parser)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    public <T> Predicate createIdInPredicate(
            @NonNull Root<T> root,
            @NonNull String fieldName,
            @NonNull List<Long> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        return root.get(fieldName).in(ids);
    }
    
    public <T> Predicate createIdInPredicateWithNestedField(
            @NonNull Root<T> root,
            @NonNull String nestedFieldName,
            @NonNull String nestedFieldId,
            @NonNull List<Long> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        return root.get(nestedFieldName).get(nestedFieldId).in(ids);
    }
    
    public <T> Predicate parseAndCreateIdPredicate(
            @NonNull List<String> values,
            @NonNull Function<String, Long> parser,
            @NonNull Root<T> root,
            @NonNull String fieldName,
            @NonNull String errorMessage) {
        
        if (isEmpty(values)) {
            return null;
        }
        
        try {
            List<Long> ids = parseLongValues(values, parser);
            return createIdInPredicate(root, fieldName, ids);
        } catch (Exception e) {
            log.error("{}: fieldName={}, values={}", errorMessage, fieldName, values, e);
            return null;
        }
    }
    
    public <T> Predicate parseAndCreateNestedIdPredicate(
            @NonNull List<String> values,
            @NonNull Function<String, Long> parser,
            @NonNull Root<T> root,
            @NonNull String nestedFieldName,
            @NonNull String nestedFieldId,
            @NonNull String errorMessage) {
        
        if (isEmpty(values)) {
            return null;
        }
        
        try {
            List<Long> ids = parseLongValues(values, parser);
            return createIdInPredicateWithNestedField(root, nestedFieldName, nestedFieldId, ids);
        } catch (Exception e) {
            log.error("{}: values={}", errorMessage, values, e);
            return null;
        }
    }
}
