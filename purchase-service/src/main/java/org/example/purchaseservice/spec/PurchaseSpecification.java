package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PurchaseSpecification implements Specification<Purchase> {
    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> clientIds;
    private final List<Long> sourceIds;

    public PurchaseSpecification(String query, Map<String, List<String>> filterParams,
                                 List<Long> clientIds, List<Long> sourceIds) {
        this.query = query;
        this.filterParams = filterParams;
        this.sourceIds = sourceIds;
        this.clientIds = clientIds;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Purchase> root, CriteriaQuery<?> query,
                                 @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = applyFilters(criteriaBuilder.conjunction(), root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (StringUtils.hasText(this.query)) {
            List<Predicate> searchPredicates = new ArrayList<>();

            searchPredicates.add(
                    criteriaBuilder.like(criteriaBuilder.toString(root.get("id")),
                            String.format("%%%s%%", this.query.toLowerCase()))
            );

            if (sourceIds != null && !sourceIds.isEmpty()) {
                searchPredicates.add(root.get("source").in(sourceIds));
            }

            if (clientIds != null && !clientIds.isEmpty()) {
                searchPredicates.add(root.get("client").in(clientIds));
            }

            Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
            predicates.add(searchPredicate);
        } else if (clientIds != null && !clientIds.isEmpty()) {
            predicates.add(root.get("client").in(clientIds));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate applyFilters(Predicate predicate, Root<Purchase> root, CriteriaBuilder criteriaBuilder) {
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            switch (key) {
                case "createdAtFrom":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "createdAt", true);
                    break;
                case "createdAtTo":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "createdAt", false);
                    break;
                case "quantityFrom":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "quantity", true);
                    break;
                case "quantityTo":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "quantity", false);
                    break;
                case "unitPriceFrom":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "unitPrice", true);
                    break;
                case "unitPriceTo":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "unitPrice", false);
                    break;
                case "totalPriceFrom":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "totalPrice", true);
                    break;
                case "totalPriceTo":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "totalPrice", false);
                    break;
                case "source":
                    predicate = addIdFilter(predicate, root, criteriaBuilder, values, "source");
                    break;
                case "product":
                    predicate = addIdFilter(predicate, root, criteriaBuilder, values, "product");
                    break;
                case "user":
                    predicate = addIdFilter(predicate, root, criteriaBuilder, values, "user");
                    break;
                case "currencyType":
                    predicate = addStringFilter(predicate, root, criteriaBuilder, values, "currency");
                    break;
                case "paymentMethod":
                    predicate = addPaymentMethodFilter(predicate, root, criteriaBuilder, values);
                    break;
                default:
                    break;
            }
        }

        return predicate;
    }


    private Predicate addIdFilter(Predicate predicate, Root<Purchase> root, CriteriaBuilder criteriaBuilder,
                                  List<String> values, String field) {
        if (!values.isEmpty()) {
            List<Long> ids = values.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            predicate = criteriaBuilder.and(predicate, root.get(field).in(ids));
        }
        return predicate;
    }

    private Predicate addStringFilter(Predicate predicate, Root<Purchase> root, CriteriaBuilder criteriaBuilder,
                                      List<String> values, String field) {
        if (!values.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, root.get(field).in(values));
        }
        return predicate;
    }

    private Predicate addPaymentMethodFilter(Predicate predicate, Root<Purchase> root, CriteriaBuilder criteriaBuilder,
                                             List<String> values) {
        if (!values.isEmpty()) {
            List<PaymentMethod> paymentMethods = values.stream()
                    .map(this::mapToPaymentMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!paymentMethods.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, root.get("paymentMethod").in(paymentMethods));
            }
        }
        return predicate;
    }

    private PaymentMethod mapToPaymentMethod(String value) {
        try {
            return switch (value) {
                case "2" -> PaymentMethod.CASH;
                case "1" -> PaymentMethod.BANKTRANSFER;
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Predicate addDateFilter(Predicate predicate, Root<Purchase> root, CriteriaBuilder criteriaBuilder,
                                    List<String> values, String field, boolean isFrom) {
        if (!values.isEmpty()) {
            LocalDate date = LocalDate.parse(values.getFirst());
            LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), dateTime) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), dateTime));
        }
        return predicate;
    }

    private Predicate addNumericFilter(Predicate predicate, Root<Purchase> root, CriteriaBuilder criteriaBuilder,
                                       List<String> values, String field, boolean isFrom) {
        if (!values.isEmpty()) {
            Double value = Double.parseDouble(values.getFirst());
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), value) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), value));
        }
        return predicate;
    }
}
