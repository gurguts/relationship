package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

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
public class PurchaseSpecification implements Specification<Purchase> {
    
    private static final String FILTER_CREATED_AT_FROM = "createdAtFrom";
    private static final String FILTER_CREATED_AT_TO = "createdAtTo";
    private static final String FILTER_QUANTITY_FROM = "quantityFrom";
    private static final String FILTER_QUANTITY_TO = "quantityTo";
    private static final String FILTER_UNIT_PRICE_FROM = "unitPriceFrom";
    private static final String FILTER_UNIT_PRICE_TO = "unitPriceTo";
    private static final String FILTER_TOTAL_PRICE_FROM = "totalPriceFrom";
    private static final String FILTER_TOTAL_PRICE_TO = "totalPriceTo";
    private static final String FILTER_SOURCE = "source";
    private static final String FILTER_PRODUCT = "product";
    private static final String FILTER_USER = "user";
    private static final String FILTER_CURRENCY = "currency";
    private static final String FILTER_CURRENCY_TYPE = "currencyType";
    private static final String FILTER_PAYMENT_METHOD = "paymentMethod";
    private static final String SUFFIX_FROM = "From";
    private static final String SUFFIX_TO = "To";
    
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_UNIT_PRICE = "unitPrice";
    private static final String FIELD_TOTAL_PRICE = "totalPrice";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_PRODUCT = "product";
    private static final String FIELD_USER = "user";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_PAYMENT_METHOD = "paymentMethod";
    private static final String FIELD_ID = "id";
    private static final String FIELD_COMMENT = "comment";
    private static final String FIELD_CLIENT = "client";
    
    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> clientIds;
    private final List<Long> sourceIds;

    public PurchaseSpecification(String query, Map<String, List<String>> filterParams,
                                 List<Long> clientIds, List<Long> sourceIds) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Collections.emptyMap();
        this.sourceIds = sourceIds != null ? sourceIds : Collections.emptyList();
        this.clientIds = clientIds != null ? clientIds : Collections.emptyList();
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Purchase> root, CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = applyFilters(criteriaBuilder.conjunction(), root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (clientIds != null && !clientIds.isEmpty()) {
            predicates.add(root.get(FIELD_CLIENT).in(clientIds));
        } else if (clientIds != null) {
            predicates.add(criteriaBuilder.disjunction());
        } else if (StringUtils.hasText(this.query)) {
            List<Predicate> searchPredicates = buildSearchPredicates(root, criteriaBuilder);
            Predicate searchPredicate = criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
            predicates.add(searchPredicate);
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private List<Predicate> buildSearchPredicates(@NonNull Root<Purchase> root, @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> searchPredicates = new ArrayList<>();

        addIdSearchPredicate(searchPredicates, root, criteriaBuilder);
        addCommentSearchPredicate(searchPredicates, root, criteriaBuilder);

        if (!sourceIds.isEmpty()) {
            searchPredicates.add(root.get(FIELD_SOURCE).in(sourceIds));
        }

        if (!clientIds.isEmpty()) {
            searchPredicates.add(root.get(FIELD_CLIENT).in(clientIds));
        }

        return searchPredicates;
    }

    private void addIdSearchPredicate(@NonNull List<Predicate> searchPredicates, 
                                      @NonNull Root<Purchase> root, 
                                      @NonNull CriteriaBuilder criteriaBuilder) {
        Long idValue = tryParseLong(this.query);
        if (idValue != null) {
            searchPredicates.add(criteriaBuilder.equal(root.get(FIELD_ID), idValue));
        } else {
            searchPredicates.add(
                    criteriaBuilder.like(criteriaBuilder.toString(root.get(FIELD_ID)),
                            String.format("%%%s%%", this.query.toLowerCase()))
            );
        }
    }

    private void addCommentSearchPredicate(@NonNull List<Predicate> searchPredicates, 
                                           @NonNull Root<Purchase> root, 
                                           @NonNull CriteriaBuilder criteriaBuilder) {
        searchPredicates.add(
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get(FIELD_COMMENT)),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(FIELD_COMMENT)),
                                String.format("%%%s%%", this.query.toLowerCase()))
                )
        );
    }

    private Long tryParseLong(@NonNull String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Predicate applyFilters(Predicate predicate, @NonNull Root<Purchase> root, @NonNull CriteriaBuilder criteriaBuilder) {
        if (filterParams.isEmpty()) {
            return predicate;
        }
        
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (key == null || values == null || values.isEmpty()) {
                continue;
            }

            try {
                predicate = applyFilter(predicate, root, criteriaBuilder, key, values);
            } catch (Exception e) {
                log.error("Error applying filter: key={}, values={}", key, values, e);
            }
        }

        return predicate;
    }

    private Predicate applyFilter(Predicate predicate, @NonNull Root<Purchase> root, 
                                  @NonNull CriteriaBuilder criteriaBuilder, 
                                  @NonNull String key, @NonNull List<String> values) {
        return switch (key) {
            case FILTER_CREATED_AT_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, true);
            case FILTER_CREATED_AT_TO -> addDateFilter(predicate, root, criteriaBuilder, values, false);
            case FILTER_QUANTITY_FROM -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_QUANTITY, true);
            case FILTER_QUANTITY_TO -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_QUANTITY, false);
            case FILTER_UNIT_PRICE_FROM -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_UNIT_PRICE, true);
            case FILTER_UNIT_PRICE_TO -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_UNIT_PRICE, false);
            case FILTER_TOTAL_PRICE_FROM -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_TOTAL_PRICE, true);
            case FILTER_TOTAL_PRICE_TO -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_TOTAL_PRICE, false);
            case FILTER_SOURCE -> addIdFilter(predicate, root, criteriaBuilder, values, FIELD_SOURCE);
            case FILTER_PRODUCT -> addIdFilter(predicate, root, criteriaBuilder, values, FIELD_PRODUCT);
            case FILTER_USER -> addIdFilter(predicate, root, criteriaBuilder, values, FIELD_USER);
            case FILTER_CURRENCY, FILTER_CURRENCY_TYPE -> addStringFilter(predicate, root, criteriaBuilder, values);
            case FILTER_PAYMENT_METHOD -> addPaymentMethodFilter(predicate, root, criteriaBuilder, values);
            default -> handleDynamicFilter(predicate, root, criteriaBuilder, key, values);
        };
    }

    private Predicate handleDynamicFilter(Predicate predicate, @NonNull Root<Purchase> root,
                                         @NonNull CriteriaBuilder criteriaBuilder,
                                         @NonNull String key, @NonNull List<String> values) {
        if (key.endsWith(SUFFIX_FROM)) {
            String fieldName = key.substring(0, key.length() - SUFFIX_FROM.length());
            return applyRangeFilter(predicate, root, criteriaBuilder, values, fieldName, true);
        } else if (key.endsWith(SUFFIX_TO)) {
            String fieldName = key.substring(0, key.length() - SUFFIX_TO.length());
            return applyRangeFilter(predicate, root, criteriaBuilder, values, fieldName, false);
        }
        return predicate;
    }

    private Predicate applyRangeFilter(Predicate predicate, @NonNull Root<Purchase> root,
                                      @NonNull CriteriaBuilder criteriaBuilder,
                                      @NonNull List<String> values, @NonNull String fieldName, boolean isFrom) {
        return switch (fieldName) {
            case FIELD_CREATED_AT -> addDateFilter(predicate, root, criteriaBuilder, values, isFrom);
            case FIELD_QUANTITY -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_QUANTITY, isFrom);
            case FIELD_UNIT_PRICE -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_UNIT_PRICE, isFrom);
            case FIELD_TOTAL_PRICE -> addNumericFilter(predicate, root, criteriaBuilder, values, FIELD_TOTAL_PRICE, isFrom);
            default -> predicate;
        };
    }

    private Predicate addIdFilter(Predicate predicate, @NonNull Root<Purchase> root, 
                                  @NonNull CriteriaBuilder criteriaBuilder,
                                  @NonNull List<String> values, @NonNull String field) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        try {
            List<Long> ids = values.stream()
                    .filter(Objects::nonNull)
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!ids.isEmpty()) {
                predicate = criteriaBuilder.and(predicate, root.get(field).in(ids));
            }
        } catch (Exception e) {
            log.error("Error parsing ID filter: field={}, values={}", field, values, e);
        }
        
        return predicate;
    }

    private Predicate addStringFilter(Predicate predicate, @NonNull Root<Purchase> root, 
                                      @NonNull CriteriaBuilder criteriaBuilder,
                                      @NonNull List<String> values) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        List<String> nonNullValues = values.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
        
        if (!nonNullValues.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, root.get(PurchaseSpecification.FIELD_CURRENCY).in(nonNullValues));
        }
        
        return predicate;
    }

    private Predicate addPaymentMethodFilter(Predicate predicate, @NonNull Root<Purchase> root, 
                                             @NonNull CriteriaBuilder criteriaBuilder,
                                             @NonNull List<String> values) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        List<PaymentMethod> paymentMethods = values.stream()
                .filter(Objects::nonNull)
                .map(this::mapToPaymentMethod)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!paymentMethods.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, root.get(FIELD_PAYMENT_METHOD).in(paymentMethods));
        }
        
        return predicate;
    }

    private PaymentMethod mapToPaymentMethod(@NonNull String value) {
        return switch (value.trim()) {
            case "2" -> PaymentMethod.CASH;
            case "1" -> PaymentMethod.BANKTRANSFER;
            default -> null;
        };
    }

    private Predicate addDateFilter(Predicate predicate, @NonNull Root<Purchase> root,
                                    @NonNull CriteriaBuilder criteriaBuilder,
                                    @NonNull List<String> values, boolean isFrom) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        try {
            String dateString = values.getFirst();
            if (dateString == null || dateString.trim().isEmpty()) {
                return predicate;
            }
            
            LocalDate date = LocalDate.parse(dateString.trim());
            LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(PurchaseSpecification.FIELD_CREATED_AT), dateTime) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(PurchaseSpecification.FIELD_CREATED_AT), dateTime));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date filter: field={}, value={}", PurchaseSpecification.FIELD_CREATED_AT, values.getFirst(), e);
        } catch (Exception e) {
            log.error("Error adding date filter: field={}, values={}", PurchaseSpecification.FIELD_CREATED_AT, values, e);
        }
        
        return predicate;
    }

    private Predicate addNumericFilter(Predicate predicate, @NonNull Root<Purchase> root, 
                                      @NonNull CriteriaBuilder criteriaBuilder,
                                      @NonNull List<String> values, @NonNull String field, boolean isFrom) {
        if (values.isEmpty()) {
            return predicate;
        }
        
        try {
            String valueString = values.getFirst();
            if (valueString == null || valueString.trim().isEmpty()) {
                return predicate;
            }
            
            Double value = Double.parseDouble(valueString.trim());
            
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), value) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), value));
        } catch (NumberFormatException e) {
            log.error("Error parsing numeric filter: field={}, value={}", field, values.getFirst(), e);
        } catch (Exception e) {
            log.error("Error adding numeric filter: field={}, values={}", field, values, e);
        }
        
        return predicate;
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
