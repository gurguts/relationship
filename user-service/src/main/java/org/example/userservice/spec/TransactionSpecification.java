package org.example.userservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.userservice.models.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionSpecification implements Specification<Transaction> {

    private final Map<String, List<String>> filters;

    public TransactionSpecification(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(@NotNull Root<Transaction> root, CriteriaQuery<?> query, @NotNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            if (filters.containsKey("target_user_id")) {
                List<Long> targetUserIds = filters.get("target_user_id").stream().map(Long::parseLong).toList();
                predicates.add(root.get("targetUserId").in(targetUserIds));
            }

            if (filters.containsKey("type")) {
                List<String> types = filters.get("type");
                predicates.add(root.get("type").in(types));
            }

            if (filters.containsKey("currency")) {
                List<String> types = filters.get("currency");
                predicates.add(root.get("currency").in(types));
            }

            if (filters.containsKey("created_at_from") || filters.containsKey("created_at_to")) {
                LocalDateTime fromDate = null;
                LocalDateTime toDate = null;

                if (filters.containsKey("created_at_from")) {
                    List<String> fromDates = filters.get("created_at_from").stream()
                            .filter(s -> !s.isEmpty())
                            .toList();
                    if (!fromDates.isEmpty()) {
                        try {
                            LocalDate date = LocalDate.parse(fromDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                            fromDate = date.atStartOfDay();
                        } catch (Exception _) {
                        }
                    }
                }

                if (filters.containsKey("created_at_to")) {
                    List<String> toDates = filters.get("created_at_to").stream()
                            .filter(s -> !s.isEmpty())
                            .toList();
                    if (!toDates.isEmpty()) {
                        try {
                            LocalDate date = LocalDate.parse(toDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                            toDate = date.atTime(23, 59, 59, 999999999);
                        } catch (Exception _) {

                        }
                    }
                }

                if (fromDate != null && toDate != null) {
                    predicates.add(cb.between(root.get("createdAt"), fromDate, toDate));
                } else if (fromDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
                } else if (toDate != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
                }
            }

            if (filters.containsKey("executor_user_id")) {
                List<Long> executorUserIds = filters.get("executor_user_id").stream().map(Long::parseLong).toList();
                predicates.add(root.get("executorUserId").in(executorUserIds));
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
