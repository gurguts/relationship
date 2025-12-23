package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseReceiptSpecification implements Specification<WarehouseReceipt> {
    private final Map<String, List<String>> filters;

    public WarehouseReceiptSpecification(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(@NotNull Root<WarehouseReceipt> root, CriteriaQuery<?> query,
                                 @NotNull CriteriaBuilder cb) {
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

            if (filters.containsKey("warehouse_id")) {
                List<Long> warehouseIds = filters.get("warehouse_id").stream().map(Long::parseLong).toList();
                predicates.add(root.get("warehouseId").in(warehouseIds));
            }

            if (filters.containsKey("type")) {
                List<Long> typeIds = filters.get("type").stream().map(Long::parseLong).toList();
                predicates.add(root.get("type").get("id").in(typeIds));
            }

            if (filters.containsKey("entry_date_from")) {
                List<String> fromDates = filters.get("entry_date_from").stream().filter(s -> !s.isEmpty()).toList();
                if (!fromDates.isEmpty()) {
                    try {
                        LocalDate fromDate = LocalDate.parse(fromDates.getFirst(), DateTimeFormatter.ISO_LOCAL_DATE);
                        predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), fromDate));
                    } catch (Exception e) {
                        throw new WarehouseException("INVALID_FILTER", String.format("Invalid entry_date_from format: %s",
                                fromDates.getFirst()));
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
                        throw new WarehouseException("INVALID_FILTER", String.format("Invalid entry_date_to format: %s",
                                toDates.getFirst()));
                    }
                }
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}

