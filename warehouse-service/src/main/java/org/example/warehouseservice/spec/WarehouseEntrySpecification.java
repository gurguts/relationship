package org.example.warehouseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.warehouseservice.models.WarehouseEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseEntrySpecification implements Specification<WarehouseEntry> {
    private final Map<String, List<String>> filters;

    public WarehouseEntrySpecification(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(Root<WarehouseEntry> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            if (filters.containsKey("user_id")) {
                List<Long> userIds = filters.get("user_id").stream().map(Long::parseLong).toList();
                predicates.add(root.get("userId").in(userIds));
            }

            if (filters.containsKey("product_id")) {
                List<Long> productIds = filters.get("product_id").stream().map(Long::parseLong).toList();
                predicates.add(root.get("productId").in(productIds));
            }

            if (filters.containsKey("entry_date_from")) {
                List<String> fromDates = filters.get("entry_date_from").stream().filter(s -> !s.isEmpty()).toList();
                if (!fromDates.isEmpty()) {
                    try {
                        LocalDate fromDate = LocalDate.parse(fromDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                        predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), fromDate));
                    } catch (Exception e) {
                        System.err.println("Invalid entry_date_from format: " + fromDates.getFirst());
                    }
                }
            }

            if (filters.containsKey("entry_date_to")) {
                List<String> toDates = filters.get("entry_date_to").stream().filter(s -> !s.isEmpty()).toList();
                if (!toDates.isEmpty()) {
                    try {
                        LocalDate toDate = LocalDate.parse(toDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                        predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), toDate));
                    } catch (Exception e) {
                        System.err.println("Invalid entry_date_to format: " + toDates.getFirst());
                    }
                }
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}