package org.example.userservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionFilterPredicateFactory {

    private static final String ERROR_CODE_INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    private static final String FILTER_TYPE = "type";
    private static final String FILTER_CURRENCY = "currency";
    private static final String FILTER_CREATED_AT_FROM = "created_at_from";
    private static final String FILTER_CREATED_AT_TO = "created_at_to";
    private static final String FILTER_EXECUTOR_USER_ID = "executor_user_id";
    private static final String FILTER_FROM_ACCOUNT_ID = "from_account_id";
    private static final String FILTER_TO_ACCOUNT_ID = "to_account_id";
    private static final String FILTER_ACCOUNT_ID = "account_id";
    private static final String FILTER_CATEGORY_ID = "category_id";

    private final TransactionFilterValueParser parser;

    public TransactionFilterPredicateFactory(@NonNull TransactionFilterValueParser parser) {
        this.parser = parser;
    }

    public List<Predicate> buildPredicates(@NonNull Root<Transaction> root, @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        addTypeFilter(root, predicates);
        addCurrencyFilter(root, predicates);
        addDateRangeFilter(root, cb, predicates);
        addExecutorUserIdFilter(root, predicates);
        addFromAccountIdFilter(root, predicates);
        addToAccountIdFilter(root, predicates);
        addAccountIdFilter(root, cb, predicates);
        addCategoryIdFilter(root, predicates);

        return predicates;
    }

    private void addTypeFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<String> types = parser.getFilterValues(FILTER_TYPE);
        if (types != null && !types.isEmpty()) {
            predicates.add(root.get("type").in(types));
        }
    }

    private void addCurrencyFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<String> currencies = parser.getFilterValues(FILTER_CURRENCY);
        if (currencies != null && !currencies.isEmpty()) {
            predicates.add(root.get("currency").in(currencies));
        }
    }

    private void addDateRangeFilter(@NonNull Root<Transaction> root, @NonNull CriteriaBuilder cb, @NonNull List<Predicate> predicates) {
        if (parser.getFilterValues(FILTER_CREATED_AT_FROM) == null && parser.getFilterValues(FILTER_CREATED_AT_TO) == null) {
            return;
        }

        LocalDateTime fromDate = parser.parseDateFromFilter(FILTER_CREATED_AT_FROM, true);
        LocalDateTime toDate = parser.parseDateFromFilter(FILTER_CREATED_AT_TO, false);

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

    private void addExecutorUserIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> userIds = parser.parseLongFilter(FILTER_EXECUTOR_USER_ID);
        if (userIds != null && !userIds.isEmpty()) {
            predicates.add(root.get("executorUserId").in(userIds));
        }
    }

    private void addFromAccountIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> accountIds = parser.parseLongFilter(FILTER_FROM_ACCOUNT_ID);
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(root.get("fromAccountId").in(accountIds));
        }
    }

    private void addToAccountIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> accountIds = parser.parseLongFilter(FILTER_TO_ACCOUNT_ID);
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(root.get("toAccountId").in(accountIds));
        }
    }

    private void addAccountIdFilter(@NonNull Root<Transaction> root, @NonNull CriteriaBuilder cb, @NonNull List<Predicate> predicates) {
        List<Long> accountIds = parser.parseLongFilter(FILTER_ACCOUNT_ID);
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(cb.or(
                    root.get("fromAccountId").in(accountIds),
                    root.get("toAccountId").in(accountIds)
            ));
        }
    }

    private void addCategoryIdFilter(@NonNull Root<Transaction> root, @NonNull List<Predicate> predicates) {
        List<Long> categoryIds = parser.parseLongFilter(FILTER_CATEGORY_ID);
        if (categoryIds != null && !categoryIds.isEmpty()) {
            predicates.add(root.get("categoryId").in(categoryIds));
        }
    }
}
