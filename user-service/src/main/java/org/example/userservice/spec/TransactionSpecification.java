package org.example.userservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TransactionSpecification implements Specification<Transaction> {
    private static final String ERROR_CODE_INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    private static final String ERROR_CODE_INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    private static final String ERROR_CODE_INVALID_ID_FORMAT = "INVALID_ID_FORMAT";
    
    private static final String FILTER_TYPE = "type";
    private static final String FILTER_CURRENCY = "currency";
    private static final String FILTER_CREATED_AT_FROM = "created_at_from";
    private static final String FILTER_CREATED_AT_TO = "created_at_to";
    private static final String FILTER_EXECUTOR_USER_ID = "executor_user_id";
    private static final String FILTER_FROM_ACCOUNT_ID = "from_account_id";
    private static final String FILTER_TO_ACCOUNT_ID = "to_account_id";
    private static final String FILTER_ACCOUNT_ID = "account_id";
    private static final String FILTER_CATEGORY_ID = "category_id";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int END_OF_DAY_HOUR = 23;
    private static final int END_OF_DAY_MINUTE = 59;
    private static final int END_OF_DAY_SECOND = 59;
    private static final int END_OF_DAY_NANO = 999999999;

    private final Map<String, List<String>> filters;

    public TransactionSpecification(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Transaction> root, CriteriaQuery<?> query, @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters == null || filters.isEmpty()) {
            return cb.and(predicates.toArray(new Predicate[0]));
        }

        addTypeFilter(root, predicates);
        addCurrencyFilter(root, predicates);
        addDateRangeFilter(root, cb, predicates);
        addExecutorUserIdFilter(root, predicates);
        addFromAccountIdFilter(root, predicates);
        addToAccountIdFilter(root, predicates);
        addAccountIdFilter(root, cb, predicates);
        addCategoryIdFilter(root, predicates);

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private void addTypeFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<String> types = getFilterValues(FILTER_TYPE);
        if (types != null && !types.isEmpty()) {
            predicates.add(root.get("type").in(types));
        }
    }

    private void addCurrencyFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<String> currencies = getFilterValues(FILTER_CURRENCY);
        if (currencies != null && !currencies.isEmpty()) {
            predicates.add(root.get("currency").in(currencies));
        }
    }

    private void addDateRangeFilter(@NonNull Root<Transaction> root, @NonNull CriteriaBuilder cb, @NonNull List<Predicate> predicates) {
        if (!filters.containsKey(FILTER_CREATED_AT_FROM) && !filters.containsKey(FILTER_CREATED_AT_TO)) {
            return;
        }

        LocalDateTime fromDate = parseDateFromFilter(FILTER_CREATED_AT_FROM, true);
        LocalDateTime toDate = parseDateFromFilter(FILTER_CREATED_AT_TO, false);

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new TransactionException(ERROR_CODE_INVALID_DATE_RANGE,
                        String.format("Date from (%s) cannot be after date to (%s)", fromDate.toLocalDate(), toDate.toLocalDate()));
            }
            predicates.add(cb.between(root.get("createdAt"), fromDate, toDate));
        } else if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        } else if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
    }

    private LocalDateTime parseDateFromFilter(@NonNull String filterKey, boolean isStartOfDay) {
        List<String> dateValues = getFilterValues(filterKey);
        if (dateValues == null || dateValues.isEmpty()) {
            return null;
        }

        String dateString = dateValues.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .findFirst()
                .orElse(null);

        if (dateString == null) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(dateString.trim(), DATE_FORMATTER);
            return isStartOfDay ? date.atStartOfDay() : date.atTime(END_OF_DAY_HOUR, END_OF_DAY_MINUTE, END_OF_DAY_SECOND, END_OF_DAY_NANO);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date from filter {}: value={}, error={}", filterKey, dateString, e.getMessage());
            throw new TransactionException(ERROR_CODE_INVALID_DATE_FORMAT,
                    String.format("Invalid date format for filter %s: %s. Expected format: yyyy-MM-dd", filterKey, dateString));
        }
    }

    private void addExecutorUserIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> userIds = parseLongFilter(FILTER_EXECUTOR_USER_ID);
        if (userIds != null && !userIds.isEmpty()) {
            predicates.add(root.get("executorUserId").in(userIds));
        }
    }

    private void addFromAccountIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> accountIds = parseLongFilter(FILTER_FROM_ACCOUNT_ID);
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(root.get("fromAccountId").in(accountIds));
        }
    }

    private void addToAccountIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> accountIds = parseLongFilter(FILTER_TO_ACCOUNT_ID);
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(root.get("toAccountId").in(accountIds));
        }
    }

    private void addAccountIdFilter(@NonNull Root<Transaction> root, @NonNull CriteriaBuilder cb, @NonNull List<Predicate> predicates) {
        List<Long> accountIds = parseLongFilter(FILTER_ACCOUNT_ID);
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(cb.or(
                    root.get("fromAccountId").in(accountIds),
                    root.get("toAccountId").in(accountIds)
            ));
        }
    }

    private void addCategoryIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> categoryIds = parseLongFilter(FILTER_CATEGORY_ID);
        if (categoryIds != null && !categoryIds.isEmpty()) {
            predicates.add(root.get("categoryId").in(categoryIds));
        }
    }

    private List<String> getFilterValues(@NonNull String filterKey) {
        return filters.get(filterKey);
    }

    private List<Long> parseLongFilter(@NonNull String filterKey) {
        List<String> values = getFilterValues(filterKey);
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(value -> {
                    try {
                        return Long.parseLong(value.trim());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse Long from filter {}: value={}, error={}", filterKey, value, e.getMessage());
                        throw new TransactionException(ERROR_CODE_INVALID_ID_FORMAT,
                                String.format("Invalid ID format for filter %s: %s", filterKey, value));
                    }
                })
                .collect(Collectors.toList());
    }
}
