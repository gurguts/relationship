package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
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
public class WarehouseWithdrawalSpecification implements Specification<WarehouseWithdrawal> {
    
    private static final String FILTER_PRODUCT_ID = "product_id";
    private static final String FILTER_WAREHOUSE_ID = "warehouse_id";
    private static final String FILTER_WITHDRAWAL_REASON_ID = "withdrawal_reason_id";
    private static final String FILTER_WITHDRAWAL_DATE_FROM = "withdrawal_date_from";
    private static final String FILTER_WITHDRAWAL_DATE_TO = "withdrawal_date_to";
    
    private static final String FIELD_PRODUCT_ID = "productId";
    private static final String FIELD_WAREHOUSE_ID = "warehouseId";
    private static final String FIELD_WITHDRAWAL_REASON = "withdrawalReason";
    private static final String FIELD_WITHDRAWAL_REASON_ID = "id";
    private static final String FIELD_WITHDRAWAL_DATE = "withdrawalDate";
    
    private final Map<String, List<String>> filters;

    public WarehouseWithdrawalSpecification(Map<String, List<String>> filters) {
        this.filters = filters != null ? filters : Collections.emptyMap();
    }

    @Override
    public Predicate toPredicate(@NonNull Root<WarehouseWithdrawal> root, CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters.isEmpty()) {
            return cb.conjunction();
        }

        applyIdFilters(predicates, root);
        applyDateFilters(predicates, root, cb);

        return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
    }

    private void applyIdFilters(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseWithdrawal> root) {
        addIdFilter(predicates, root, FILTER_PRODUCT_ID, FIELD_PRODUCT_ID);
        addIdFilter(predicates, root, FILTER_WAREHOUSE_ID, FIELD_WAREHOUSE_ID);
        addWithdrawalReasonIdFilter(predicates, root);
    }

    private void addIdFilter(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseWithdrawal> root,
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

    private void addWithdrawalReasonIdFilter(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseWithdrawal> root) {
        List<String> values = filters.get(FILTER_WITHDRAWAL_REASON_ID);
        if (values == null || values.isEmpty()) {
            return;
        }

        try {
            List<Long> withdrawalReasonIds = values.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!withdrawalReasonIds.isEmpty()) {
                predicates.add(root.get(FIELD_WITHDRAWAL_REASON).get(FIELD_WITHDRAWAL_REASON_ID).in(withdrawalReasonIds));
            }
        } catch (Exception e) {
            log.error("Error parsing withdrawal reason ID filter: values={}", values, e);
        }
    }

    private void applyDateFilters(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseWithdrawal> root,
                                  @NonNull CriteriaBuilder cb) {
        addDateFilter(predicates, root, cb, FILTER_WITHDRAWAL_DATE_FROM, true);
        addDateFilter(predicates, root, cb, FILTER_WITHDRAWAL_DATE_TO, false);
    }

    private void addDateFilter(@NonNull List<Predicate> predicates, @NonNull Root<WarehouseWithdrawal> root,
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
                    cb.greaterThanOrEqualTo(root.get(WarehouseWithdrawalSpecification.FIELD_WITHDRAWAL_DATE), dateTime) :
                    cb.lessThanOrEqualTo(root.get(WarehouseWithdrawalSpecification.FIELD_WITHDRAWAL_DATE), dateTime));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date filter: filterKey={}, fieldName={}, value={}", filterKey, WarehouseWithdrawalSpecification.FIELD_WITHDRAWAL_DATE, values, e);
        } catch (Exception e) {
            log.error("Error adding date filter: filterKey={}, fieldName={}, values={}", filterKey, WarehouseWithdrawalSpecification.FIELD_WITHDRAWAL_DATE, values, e);
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