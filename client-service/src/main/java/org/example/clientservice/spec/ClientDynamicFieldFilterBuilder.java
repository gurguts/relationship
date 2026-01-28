package org.example.clientservice.spec;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.clientservice.spec.ClientSubqueryBuilder.buildWherePredicates;
import static org.example.clientservice.spec.ClientSubqueryBuilder.createFieldValueSubquery;
import static org.example.clientservice.spec.ClientFilterValueParser.*;

@Slf4j
public class ClientDynamicFieldFilterBuilder {

    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_ID = "id";
    private static final String FIELD_FIELD_NAME = "fieldName";
    private static final String FIELD_VALUE_TEXT = "valueText";
    private static final String FIELD_VALUE_BOOLEAN = "valueBoolean";
    private static final String FIELD_VALUE_DATE = "valueDate";
    private static final String FIELD_VALUE_NUMBER = "valueNumber";
    private static final String FIELD_VALUE_LIST = "valueList";
    private static final String FIELD_VALUE = "value";
    private static final String VALUE_TRUE = "true";

    public static Predicate addDynamicFieldSearchPredicate(Root<Client> root, CriteriaQuery<?> query, 
                                                          CriteriaBuilder criteriaBuilder,
                                                          Predicate keywordPredicate,
                                                          String searchQuery,
                                                          Long clientTypeId) {
        SubqueryContext subqueryContext = createFieldValueSubquery(query);
        
        Predicate clientPredicate = criteriaBuilder.equal(subqueryContext.fieldValueRoot().get(FIELD_CLIENT), root);
        Predicate searchablePredicate = criteriaBuilder.isTrue(subqueryContext.fieldJoin().get("isSearchable").as(Boolean.class));
        List<Predicate> basePredicates = buildBasePredicates(
                criteriaBuilder, subqueryContext, clientPredicate, searchablePredicate, clientTypeId);

        String searchPattern = createSearchPattern(searchQuery);

        Predicate textSearchPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(subqueryContext.fieldValueRoot().get(FIELD_VALUE_TEXT)),
                searchPattern
        );

        Join<ClientFieldValue, ?> valueListJoin = subqueryContext.fieldValueRoot().join(
                FIELD_VALUE_LIST, jakarta.persistence.criteria.JoinType.LEFT);
        Predicate listValueSearchPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(valueListJoin.get(FIELD_VALUE)),
                searchPattern
        );

        Predicate dynamicFieldPredicate = criteriaBuilder.or(textSearchPredicate, listValueSearchPredicate);
        basePredicates.add(dynamicFieldPredicate);
        
        subqueryContext.subquery().where(criteriaBuilder.and(basePredicates.toArray(new Predicate[0])));
        
        return criteriaBuilder.or(
                keywordPredicate,
                criteriaBuilder.exists(subqueryContext.subquery())
        );
    }

    public static Predicate addDynamicFieldFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder criteriaBuilder, String fieldName, 
                                                 List<String> values, Long clientTypeId) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        
        SubqueryContext subqueryContext = createFieldValueSubquery(query);

        Predicate fieldNamePredicate = criteriaBuilder.equal(
            criteriaBuilder.lower(subqueryContext.fieldJoin().get(FIELD_FIELD_NAME)), 
            fieldName.toLowerCase()
        );

        List<Predicate> basePredicates = buildBasePredicates(
                criteriaBuilder, subqueryContext, fieldNamePredicate, null, clientTypeId);

        return applyValueFilterPredicate(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
    }

    public static Predicate addDynamicFieldFilterById(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                     CriteriaBuilder criteriaBuilder, Long fieldId, 
                                                     List<String> values, Long clientTypeId) {
        FilterContext context = createFilterContextWithFieldId(query, values, predicate, fieldId, criteriaBuilder, clientTypeId);
        if (context == null) {
            return predicate;
        }

        return applyValueFilterPredicate(predicate, root, criteriaBuilder, context.subqueryContext(), context.basePredicates(), values);
    }

    public static Predicate addDynamicFieldRangeFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                      CriteriaBuilder criteriaBuilder, Long fieldId, 
                                                      List<String> values, Long clientTypeId, boolean isFrom) {
        FilterContext context = createFilterContextWithFieldId(query, values, predicate, fieldId, criteriaBuilder, clientTypeId);
        if (context == null) {
            return predicate;
        }
        
        return applyRangeFilter(predicate, root, criteriaBuilder, context, values, fieldId, isFrom);
    }

    private static Predicate applyValueFilterPredicate(Predicate predicate, Root<Client> root, 
                                                     CriteriaBuilder criteriaBuilder,
                                                     SubqueryContext subqueryContext, 
                                                     List<Predicate> basePredicates, 
                                                     List<String> values) {
        FieldType fieldType = determineFieldType(values);
        
        return switch (fieldType) {
            case BOOLEAN -> applyBooleanFilter(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
            case LIST -> applyListFilter(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
            case TEXT -> applyTextFilter(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
            case UNKNOWN -> predicate;
        };
    }

    private static FieldType determineFieldType(List<String> values) {
        if (values == null || values.isEmpty()) {
            return FieldType.UNKNOWN;
        }
        
        boolean allBoolean = true;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String trimmedValue = value.trim().toLowerCase();
                if (!VALUE_TRUE.equals(trimmedValue) && !"false".equals(trimmedValue)) {
                    allBoolean = false;
                    break;
                }
            }
        }
        
        if (allBoolean && hasNonEmptyValues(values)) {
            return FieldType.BOOLEAN;
        }
        
        boolean allNumeric = true;
        List<Long> listValueIds = new ArrayList<>();
        try {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    listValueIds.add(Long.parseLong(value.trim()));
                }
            }
            if (listValueIds.isEmpty()) {
                allNumeric = false;
            }
        } catch (NumberFormatException e) {
            allNumeric = false;
        }
        
        if (allNumeric) {
            return FieldType.LIST;
        }
        
        return FieldType.TEXT;
    }

    private static boolean hasNonEmptyValues(List<String> values) {
        return values.stream().anyMatch(v -> v != null && !v.trim().isEmpty());
    }


    private static FilterContext createFilterContextWithFieldId(CriteriaQuery<?> query, List<String> values, 
                                                               Predicate predicate, Long fieldId,
                                                               CriteriaBuilder criteriaBuilder, Long clientTypeId) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        SubqueryContext subqueryContext = createFieldValueSubquery(query);
        Predicate fieldIdPredicate = criteriaBuilder.equal(subqueryContext.fieldJoin().get(FIELD_ID), fieldId);
        List<Predicate> basePredicates = buildBasePredicates(
                criteriaBuilder, subqueryContext, fieldIdPredicate, null, clientTypeId);
        
        return new FilterContext(subqueryContext, basePredicates, predicate);
    }

    private static Predicate applyRangeFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                             FilterContext context, List<String> values, Long fieldId, boolean isFrom) {
        try {
            LocalDate date = parseDate(values);
            if (date != null) {
                return applyRangePredicate(predicate, root, criteriaBuilder, context, 
                        criteriaBuilder.greaterThanOrEqualTo(context.subqueryContext().fieldValueRoot().get(FIELD_VALUE_DATE), date),
                        criteriaBuilder.lessThanOrEqualTo(context.subqueryContext().fieldValueRoot().get(FIELD_VALUE_DATE), date),
                        isFrom);
            }
        } catch (DateTimeParseException e) {
            try {
                BigDecimal number = new BigDecimal(values.getFirst().trim());
                return applyRangePredicate(predicate, root, criteriaBuilder, context,
                        criteriaBuilder.greaterThanOrEqualTo(context.subqueryContext().fieldValueRoot().get(FIELD_VALUE_NUMBER), number),
                        criteriaBuilder.lessThanOrEqualTo(context.subqueryContext().fieldValueRoot().get(FIELD_VALUE_NUMBER), number),
                        isFrom);
            } catch (NumberFormatException ex) {
                log.warn("Invalid range filter value format for field {}: {}", fieldId, values);
            }
        }
        
        return predicate;
    }

    private static Predicate applyRangePredicate(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                                FilterContext context, Predicate greaterOrEqual, Predicate lessOrEqual,
                                                boolean isFrom) {
        Predicate rangePredicate = isFrom ? greaterOrEqual : lessOrEqual;
        
        List<Predicate> wherePredicates = buildWherePredicates(context.basePredicates(), 
                context.subqueryContext().fieldValueRoot(), root, criteriaBuilder, rangePredicate);
        
        context.subqueryContext().subquery().where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(context.subqueryContext().subquery()));
    }

    private static List<Predicate> buildBasePredicates(CriteriaBuilder criteriaBuilder,
                                                       SubqueryContext subqueryContext,
                                                       Predicate fieldPredicate,
                                                       Predicate additionalPredicate,
                                                       Long clientTypeId) {
        List<Predicate> basePredicates = new ArrayList<>();
        basePredicates.add(fieldPredicate);
        if (additionalPredicate != null) {
            basePredicates.add(additionalPredicate);
        }
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(subqueryContext.clientTypeJoin().get(FIELD_ID), clientTypeId));
        }
        return basePredicates;
    }

    private record FilterContext(
            SubqueryContext subqueryContext,
            List<Predicate> basePredicates,
            Predicate originalPredicate
    ) {
    }

    private static Predicate applyBooleanFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                               SubqueryContext subqueryContext, List<Predicate> basePredicates, 
                                               List<String> values) {
        List<Boolean> booleanValues = values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(v -> VALUE_TRUE.equalsIgnoreCase(v.trim()))
                .collect(Collectors.toList());
        
        if (booleanValues.isEmpty()) {
            return predicate;
        }
        
        Predicate booleanPredicate = subqueryContext.fieldValueRoot().get(FIELD_VALUE_BOOLEAN).in(booleanValues);
        List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot(), root, 
                criteriaBuilder, booleanPredicate);
        
        subqueryContext.subquery().where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery()));
    }

    private static Predicate applyListFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                            SubqueryContext subqueryContext, List<Predicate> basePredicates, 
                                            List<String> values) {
        List<Long> listValueIds = new ArrayList<>();
        try {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    listValueIds.add(Long.parseLong(value.trim()));
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid list value ID format: {}", values);
            return predicate;
        }
        
        if (listValueIds.isEmpty()) {
            return predicate;
        }
        
        Join<ClientFieldValue, ?> valueListJoin = subqueryContext.fieldValueRoot().join(
                FIELD_VALUE_LIST, jakarta.persistence.criteria.JoinType.LEFT);
        Predicate valuePredicate = valueListJoin.get(FIELD_ID).in(listValueIds);
        
        List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot(), root, 
                criteriaBuilder, valuePredicate);
        
        subqueryContext.subquery().where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery()));
    }

    private static Predicate applyTextFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                            SubqueryContext subqueryContext, List<Predicate> basePredicates, 
                                            List<String> values) {
        List<Predicate> textPredicates = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String searchPattern = createSearchPattern(value);
                textPredicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(subqueryContext.fieldValueRoot().get(FIELD_VALUE_TEXT)),
                                searchPattern
                        )
                );
            }
        }
        
        if (textPredicates.isEmpty()) {
            return predicate;
        }
        
        Predicate textPredicate = criteriaBuilder.or(textPredicates.toArray(new Predicate[0]));
        List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot(), root, 
                criteriaBuilder, textPredicate);
        
        subqueryContext.subquery().where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery()));
    }

    private enum FieldType {
        BOOLEAN, LIST, TEXT, UNKNOWN
    }
}
