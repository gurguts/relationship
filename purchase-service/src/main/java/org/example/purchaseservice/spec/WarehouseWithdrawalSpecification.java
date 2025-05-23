package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.WarehouseWithdrawal;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseWithdrawalSpecification implements Specification<WarehouseWithdrawal> {
    private final Map<String, List<String>> filters;

    public WarehouseWithdrawalSpecification(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(@NotNull Root<WarehouseWithdrawal> root, CriteriaQuery<?> query,
                                 @NotNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            if (filters.containsKey("product_id")) {
                List<Long> productIds = filters.get("product_id").stream().map(Long::parseLong).toList();
                predicates.add(root.get("productId").in(productIds));
            }

            if (filters.containsKey("reason_type")) {
                List<String> reasonTypes = filters.get("reason_type");
                predicates.add(root.get("reasonType").in(reasonTypes));
            }

            if (filters.containsKey("withdrawal_date_from")) {
                List<String> fromDates = filters.get("withdrawal_date_from").stream().filter(s -> !s.isEmpty()).toList();
                if (!fromDates.isEmpty()) {
                    try {
                        LocalDate fromDate = LocalDate.parse(fromDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                        predicates.add(cb.greaterThanOrEqualTo(root.get("withdrawalDate"), fromDate));
                    } catch (Exception e) {
                        throw new WarehouseException(String.format("Invalid withdrawal_date_from format: %s",
                                fromDates.getFirst()));
                    }
                }
            }

            if (filters.containsKey("withdrawal_date_to")) {
                List<String> toDates = filters.get("withdrawal_date_to").stream().filter(s -> !s.isEmpty()).toList();
                if (!toDates.isEmpty()) {
                    try {
                        LocalDate toDate = LocalDate.parse(toDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                        predicates.add(cb.lessThanOrEqualTo(root.get("withdrawalDate"), toDate));
                    } catch (Exception e) {
                        throw new WarehouseException(String.format("Invalid withdrawal_date_to format: %s",
                                toDates.getFirst()));
                    }
                }
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}