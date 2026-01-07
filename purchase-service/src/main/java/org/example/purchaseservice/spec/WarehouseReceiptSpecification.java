package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class WarehouseReceiptSpecification implements Specification<WarehouseReceipt> {
    
    private static final String FILTER_USER_ID = "user_id";
    private static final String FILTER_PRODUCT_ID = "product_id";
    private static final String FILTER_WAREHOUSE_ID = "warehouse_id";
    private static final String FILTER_TYPE = "type";
    private static final String FILTER_ENTRY_DATE_FROM = "entry_date_from";
    private static final String FILTER_ENTRY_DATE_TO = "entry_date_to";
    
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PRODUCT_ID = "productId";
    private static final String FIELD_WAREHOUSE_ID = "warehouseId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TYPE_ID = "id";
    private static final String FIELD_ENTRY_DATE = "entryDate";
    
    private final Map<String, List<String>> filters;

    public WarehouseReceiptSpecification(Map<String, List<String>> filters) {
        this.filters = filters != null ? filters : Collections.emptyMap();
    }

    @Override
    public Predicate toPredicate(@NonNull Root<WarehouseReceipt> root, CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters.isEmpty()) {
            return cb.conjunction();
        }

        applyIdFilters(predicates, root, cb);
        applyDateFilters(predicates, root, cb);

        return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
    }

    private void applyIdFilters(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseReceipt> root,
                                @NonNull CriteriaBuilder cb) {
        addIdFilter(predicates, root, FILTER_USER_ID, FIELD_USER_ID);
        addIdFilter(predicates, root, FILTER_PRODUCT_ID, FIELD_PRODUCT_ID);
        addIdFilter(predicates, root, FILTER_WAREHOUSE_ID, FIELD_WAREHOUSE_ID);
        addTypeIdFilter(predicates, root);
    }

    private void addIdFilter(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseReceipt> root,
                             @NonNull String filterKey, @NonNull String fieldName) {
        List<String> values = filters.get(filterKey);
        if (values == null || values.isEmpty()) {
            return;
        }

        try {
            List<Long> ids = values.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!ids.isEmpty()) {
                predicates.add(root.get(fieldName).in(ids));
            }
        } catch (Exception e) {
            log.error("Error parsing ID filter: filterKey={}, fieldName={}, values={}", filterKey, fieldName, values, e);
        }
    }

    private void addTypeIdFilter(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseReceipt> root) {
        List<String> values = filters.get(FILTER_TYPE);
        if (values == null || values.isEmpty()) {
            return;
        }

        try {
            List<Long> typeIds = values.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!typeIds.isEmpty()) {
                predicates.add(root.get(FIELD_TYPE).get(FIELD_TYPE_ID).in(typeIds));
            }
        } catch (Exception e) {
            log.error("Error parsing type ID filter: values={}", values, e);
        }
    }

    private void applyDateFilters(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseReceipt> root,
                                  @NonNull CriteriaBuilder cb) {
        addDateFilter(predicates, root, cb, FILTER_ENTRY_DATE_FROM, true);
        addDateFilter(predicates, root, cb, FILTER_ENTRY_DATE_TO, false);
    }

    private void addDateFilter(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseReceipt> root,
                               @NonNull CriteriaBuilder cb, @NonNull String filterKey,
                               boolean isFrom) {
        List<String> values = filters.get(filterKey);
        if (values == null || values.isEmpty()) {
            return;
        }

        try {
            String dateString = values.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .findFirst()
                    .orElse(null);

            if (dateString == null) {
                return;
            }

            LocalDate date = LocalDate.parse(dateString.trim());
            LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);

            predicates.add(isFrom ?
                    cb.greaterThanOrEqualTo(root.get(WarehouseReceiptSpecification.FIELD_ENTRY_DATE), dateTime) :
                    cb.lessThanOrEqualTo(root.get(WarehouseReceiptSpecification.FIELD_ENTRY_DATE), dateTime));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date filter: filterKey={}, fieldName={}, value={}", filterKey, WarehouseReceiptSpecification.FIELD_ENTRY_DATE, values, e);
        } catch (Exception e) {
            log.error("Error adding date filter: filterKey={}, fieldName={}, values={}", filterKey, WarehouseReceiptSpecification.FIELD_ENTRY_DATE, values, e);
        }
    }

    private Long parseLong(@NonNull String value) {
        try {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            log.error("Error parsing Long value: {}", value, e);
            return null;
        }
    }
}

