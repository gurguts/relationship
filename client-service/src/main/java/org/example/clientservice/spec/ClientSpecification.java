package org.example.clientservice.spec;

import jakarta.persistence.criteria.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ClientSpecification implements Specification<Client> {
    private static final String FIELD_PREFIX = "field_";
    private static final String SUFFIX_FROM = "From";
    private static final String SUFFIX_TO = "To";
    private static final int FIELD_PREFIX_LENGTH = 6;
    
    private static final String FIELD_CLIENT_TYPE = "clientType";
    private static final String FIELD_IS_ACTIVE = "isActive";
    private static final String FIELD_ID = "id";
    private static final String FIELD_COMPANY = "company";
    private static final String FIELD_SOURCE_ID = "sourceId";
    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_FIELD = "field";
    private static final String FIELD_FIELD_NAME = "fieldName";
    private static final String FIELD_IS_SEARCHABLE = "isSearchable";
    private static final String FIELD_VALUE_TEXT = "valueText";
    private static final String FIELD_VALUE_BOOLEAN = "valueBoolean";
    private static final String FIELD_VALUE_DATE = "valueDate";
    private static final String FIELD_VALUE_NUMBER = "valueNumber";
    private static final String FIELD_VALUE_LIST = "valueList";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    
    private static final String FILTER_SHOW_INACTIVE = "showInactive";
    private static final String FILTER_SOURCE = "source";
    private static final String FILTER_CLIENT_TYPE_ID = "clientTypeId";
    private static final String FILTER_CREATED_AT_FROM = "createdAtFrom";
    private static final String FILTER_CREATED_AT_TO = "createdAtTo";
    private static final String FILTER_UPDATED_AT_FROM = "updatedAtFrom";
    private static final String FILTER_UPDATED_AT_TO = "updatedAtTo";
    
    private static final String VALUE_TRUE = "true";
    private static final String VALUE_FALSE = "false";
    private static final String LIKE_PATTERN = "%%%s%%";
    
    private static final Set<String> VALID_FILTER_KEYS = Set.of(
            FILTER_CREATED_AT_FROM, FILTER_CREATED_AT_TO, FILTER_UPDATED_AT_FROM, FILTER_UPDATED_AT_TO, 
            FILTER_SOURCE, FILTER_SHOW_INACTIVE);

    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> sourceIds;
    private final Long clientTypeId;
    private final List<Long> allowedClientTypeIds;

    public ClientSpecification(String query, Map<String, List<String>> filterParams,
                               List<Long> sourceIds,
                               Long clientTypeId) {
        this(query, filterParams, sourceIds, clientTypeId, null);
    }

    public ClientSpecification(String query, Map<String, List<String>> filterParams,
                               List<Long> sourceIds,
                               Long clientTypeId,
                               List<Long> allowedClientTypeIds) {
        this.query = query;
        this.filterParams = filterParams;
        this.sourceIds = sourceIds;
        this.clientTypeId = clientTypeId;
        this.allowedClientTypeIds = allowedClientTypeIds;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Client> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Predicate predicate = criteriaBuilder.conjunction();

        predicate = applyQueryFilter(predicate, root, query, criteriaBuilder);
        predicate = applyActiveFilter(predicate, root, criteriaBuilder);
        predicate = applyClientTypeFilters(predicate, root, criteriaBuilder);
        predicate = applyFilters(predicate, root, query, criteriaBuilder);

        return predicate;
    }

    private Predicate applyQueryFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                      CriteriaBuilder criteriaBuilder) {
        if (StringUtils.hasText(this.query)) {
            predicate = criteriaBuilder.and(predicate, createKeywordPredicate(root, query, criteriaBuilder));
        }
        return predicate;
    }

    private Predicate applyActiveFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder) {
        if (isShowInactive()) {
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.isFalse(root.get(FIELD_IS_ACTIVE)));
        } else {
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(root.get(FIELD_IS_ACTIVE)));
        }
        return predicate;
    }

    private Predicate applyClientTypeFilters(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder) {
        Path<?> clientTypePath = getClientTypePath(root);

        if (allowedClientTypeIds != null && !allowedClientTypeIds.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, 
                clientTypePath.get(FIELD_ID).in(allowedClientTypeIds));
        }

        if (clientTypeId != null) {
            predicate = criteriaBuilder.and(predicate, 
                criteriaBuilder.equal(clientTypePath.get(FIELD_ID), clientTypeId));
        }

        return predicate;
    }

    private boolean isShowInactive() {
        if (filterParams == null) {
            return false;
        }
        List<String> showInactiveValues = filterParams.get(FILTER_SHOW_INACTIVE);
        return showInactiveValues != null && !showInactiveValues.isEmpty() 
                && VALUE_TRUE.equals(showInactiveValues.getFirst());
    }

    private Path<?> getClientTypePath(Root<Client> root) {
        Join<?, ?> existingJoin = findExistingJoin(root);
        if (existingJoin != null) {
            return existingJoin;
        }

        if (hasFetch(root)) {
            return root.get(FIELD_CLIENT_TYPE);
        }

        return root.join(FIELD_CLIENT_TYPE, JoinType.LEFT);
    }

    private Join<?, ?> findExistingJoin(Root<Client> root) {
        for (Join<?, ?> join : root.getJoins()) {
            if (ClientSpecification.FIELD_CLIENT_TYPE.equals(join.getAttribute().getName())) {
                return join;
            }
        }
        return null;
    }

    private boolean hasFetch(Root<Client> root) {
        for (Fetch<?, ?> fetch : root.getFetches()) {
            if (ClientSpecification.FIELD_CLIENT_TYPE.equals(fetch.getAttribute().getName())) {
                return true;
            }
        }
        return false;
    }

    private Predicate createKeywordPredicate(Root<Client> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
        String escapedQuery = escapeQuery(this.query);
        String searchPattern = String.format(LIKE_PATTERN, escapedQuery);
        
        Predicate keywordPredicate = criteriaBuilder.or(
                criteriaBuilder.like(
                        criteriaBuilder.toString(root.get(FIELD_ID)), searchPattern),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(FIELD_COMPANY)), searchPattern)
        );

        keywordPredicate = addJoinPredicates(root, criteriaBuilder, keywordPredicate);
        keywordPredicate = addDynamicFieldSearchPredicate(root, query, criteriaBuilder, keywordPredicate);

        return keywordPredicate;
    }

    private String escapeQuery(String query) {
        return query.toLowerCase().replace("%", "\\%").replace("_", "\\_");
    }

    private Predicate addJoinPredicates(Root<Client> root, CriteriaBuilder criteriaBuilder,
                                        Predicate keywordPredicate) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return keywordPredicate;
        }
        return criteriaBuilder.or(
                keywordPredicate,
                root.get(FIELD_SOURCE_ID).in(sourceIds)
        );
    }

    private Predicate addDynamicFieldSearchPredicate(Root<Client> root, CriteriaQuery<?> query, 
                                                     CriteriaBuilder criteriaBuilder,
                                                     Predicate keywordPredicate) {
        SubqueryContext subqueryContext = createFieldValueSubquery(query);
        
        List<Predicate> basePredicates = new ArrayList<>();
        basePredicates.add(criteriaBuilder.equal(subqueryContext.fieldValueRoot.get(FIELD_CLIENT), root));
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(subqueryContext.clientTypeJoin.get(FIELD_ID), clientTypeId));
        }
        basePredicates.add(criteriaBuilder.isTrue(subqueryContext.fieldJoin.get(FIELD_IS_SEARCHABLE)));
        
        String escapedQuery = escapeQuery(this.query);
        String searchPattern = String.format(LIKE_PATTERN, escapedQuery);

        Predicate textSearchPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(subqueryContext.fieldValueRoot.get(FIELD_VALUE_TEXT)),
                searchPattern
        );

        Join<ClientFieldValue, ?> valueListJoin = subqueryContext.fieldValueRoot.join(
                FIELD_VALUE_LIST, jakarta.persistence.criteria.JoinType.LEFT);
        Predicate listValueSearchPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(valueListJoin.get(FIELD_VALUE)),
                searchPattern
        );

        Predicate dynamicFieldPredicate = criteriaBuilder.or(textSearchPredicate, listValueSearchPredicate);
        basePredicates.add(dynamicFieldPredicate);
        
        subqueryContext.subquery.where(criteriaBuilder.and(basePredicates.toArray(new Predicate[0])));
        
        return criteriaBuilder.or(
                keywordPredicate,
                criteriaBuilder.exists(subqueryContext.subquery)
        );
    }

    private Predicate applyFilters(Predicate predicate, Root<Client> root, CriteriaQuery<?> query, 
                                  CriteriaBuilder criteriaBuilder) {
        if (filterParams == null || filterParams.isEmpty()) {
            return predicate;
        }

        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (VALID_FILTER_KEYS.contains(key)) {
                predicate = applyValidFilter(predicate, root, criteriaBuilder, key, values);
            } else if (key.startsWith(FIELD_PREFIX)) {
                predicate = applyDynamicFieldFilter(predicate, root, query, criteriaBuilder, key, values);
            } else if (isDynamicFieldKey(key)) {
                predicate = addDynamicFieldFilter(predicate, root, query, criteriaBuilder, key, values, clientTypeId);
            }
        }

        return predicate;
    }

    private Predicate applyValidFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                      String key, List<String> values) {
        return switch (key) {
            case FILTER_CREATED_AT_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_CREATED_AT, true);
            case FILTER_CREATED_AT_TO -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_CREATED_AT, false);
            case FILTER_UPDATED_AT_FROM -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_UPDATED_AT, true);
            case FILTER_UPDATED_AT_TO -> addDateFilter(predicate, root, criteriaBuilder, values, FIELD_UPDATED_AT, false);
            case FILTER_SOURCE -> addSourceIdFilter(predicate, root, criteriaBuilder, values);
            default -> predicate;
        };
    }

    private boolean isDynamicFieldKey(String key) {
        return !key.endsWith(SUFFIX_FROM) && !key.endsWith(SUFFIX_TO) 
                && !key.equals(FILTER_SHOW_INACTIVE) && !key.equals(FILTER_CLIENT_TYPE_ID);
    }

    private Predicate applyDynamicFieldFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder, String key, List<String> values) {
        if (key.endsWith(SUFFIX_FROM)) {
            Long fieldId = extractFieldIdFromKey(key, SUFFIX_FROM.length());
            if (fieldId != null) {
                return addDynamicFieldRangeFilter(predicate, root, query, criteriaBuilder, fieldId, values, clientTypeId, true);
            }
        } else if (key.endsWith(SUFFIX_TO)) {
            Long fieldId = extractFieldIdFromKey(key, SUFFIX_TO.length());
            if (fieldId != null) {
                return addDynamicFieldRangeFilter(predicate, root, query, criteriaBuilder, fieldId, values, clientTypeId, false);
            }
        } else {
            Long fieldId = extractFieldIdFromKey(key, 0);
            if (fieldId != null) {
                return addDynamicFieldFilterById(predicate, root, query, criteriaBuilder, fieldId, values, clientTypeId);
            }
        }
        return predicate;
    }

    private Long extractFieldIdFromKey(String key, int suffixLength) {
        try {
            int endIndex = suffixLength > 0 ? key.length() - suffixLength : key.length();
            String fieldIdStr = key.substring(FIELD_PREFIX_LENGTH, endIndex);
            return Long.parseLong(fieldIdStr);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.warn("Invalid field ID format in key: {}", key);
            return null;
        }
    }

    private Predicate addSourceIdFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                       List<String> values) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            List<Long> ids = values.stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            predicate = criteriaBuilder.and(predicate, root.get(FIELD_SOURCE_ID).in(ids));
        } catch (NumberFormatException e) {
            throw new ClientException("INVALID_FILTER", 
                String.format("Incorrect ID format in filter %s: %s", FILTER_SOURCE, values));
        }
        return predicate;
    }

    private Predicate addDateFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                    List<String> values, String field, boolean isFrom) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            LocalDate date = LocalDate.parse(values.getFirst().trim());
            LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), dateTime) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), dateTime));
        } catch (DateTimeParseException e) {
            throw new ClientException("INVALID_FILTER", 
                String.format("Incorrect date format in filter %s: %s", field, values));
        }
        return predicate;
    }

    private Predicate addDynamicFieldFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                            CriteriaBuilder criteriaBuilder, String fieldName, List<String> values, 
                                            Long clientTypeId) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        
        SubqueryContext subqueryContext = createFieldValueSubquery(query);

        Predicate fieldNamePredicate = criteriaBuilder.equal(
            criteriaBuilder.lower(subqueryContext.fieldJoin.get(FIELD_FIELD_NAME)), 
            fieldName.toLowerCase()
        );

        List<Predicate> basePredicates = new ArrayList<>();
        basePredicates.add(fieldNamePredicate);
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(subqueryContext.clientTypeJoin.get(FIELD_ID), clientTypeId));
        }

        return applyValueFilterPredicate(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
    }

    private Predicate addDynamicFieldFilterById(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                CriteriaBuilder criteriaBuilder, Long fieldId, List<String> values, 
                                                Long clientTypeId) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        
        SubqueryContext subqueryContext = createFieldValueSubquery(query);

        Predicate fieldIdPredicate = criteriaBuilder.equal(subqueryContext.fieldJoin.get(FIELD_ID), fieldId);

        List<Predicate> basePredicates = new ArrayList<>();
        basePredicates.add(fieldIdPredicate);
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(subqueryContext.clientTypeJoin.get(FIELD_ID), clientTypeId));
        }

        return applyValueFilterPredicate(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
    }

    private SubqueryContext createFieldValueSubquery(CriteriaQuery<?> query) {
        Subquery<Long> fieldValueSubquery = query.subquery(Long.class);
        Root<ClientFieldValue> fieldValueRoot = fieldValueSubquery.from(ClientFieldValue.class);
        Join<ClientFieldValue, ClientTypeField> fieldJoin = fieldValueRoot.join(FIELD_FIELD);
        Join<ClientTypeField, ?> clientTypeJoin = fieldJoin.join(FIELD_CLIENT_TYPE);
        
        fieldValueSubquery.select(fieldValueRoot.get(FIELD_CLIENT).get(FIELD_ID));
        
        return new SubqueryContext(fieldValueSubquery, fieldValueRoot, fieldJoin, clientTypeJoin);
    }

    private Predicate applyValueFilterPredicate(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                                SubqueryContext subqueryContext, List<Predicate> basePredicates, 
                                                List<String> values) {
        FieldType fieldType = determineFieldType(values);
        
        return switch (fieldType) {
            case BOOLEAN -> applyBooleanFilter(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
            case LIST -> applyListFilter(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
            case TEXT -> applyTextFilter(predicate, root, criteriaBuilder, subqueryContext, basePredicates, values);
            case UNKNOWN -> predicate;
        };
    }

    private FieldType determineFieldType(List<String> values) {
        if (values == null || values.isEmpty()) {
            return FieldType.UNKNOWN;
        }
        
        boolean allBoolean = true;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String trimmedValue = value.trim().toLowerCase();
                if (!VALUE_TRUE.equals(trimmedValue) && !VALUE_FALSE.equals(trimmedValue)) {
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

    private boolean hasNonEmptyValues(List<String> values) {
        return values.stream().anyMatch(v -> v != null && !v.trim().isEmpty());
    }

    private Predicate applyBooleanFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                        SubqueryContext subqueryContext, List<Predicate> basePredicates, 
                                        List<String> values) {
        List<Boolean> booleanValues = values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(v -> VALUE_TRUE.equalsIgnoreCase(v.trim()))
                .collect(Collectors.toList());
        
        if (booleanValues.isEmpty()) {
            return predicate;
        }
        
        Predicate booleanPredicate = subqueryContext.fieldValueRoot.get(FIELD_VALUE_BOOLEAN).in(booleanValues);
        List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot, root, 
                criteriaBuilder, booleanPredicate);
        
        subqueryContext.subquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery));
    }

    private Predicate applyListFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
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
        
        Join<ClientFieldValue, ?> valueListJoin = subqueryContext.fieldValueRoot.join(
                FIELD_VALUE_LIST, jakarta.persistence.criteria.JoinType.LEFT);
        Predicate valuePredicate = valueListJoin.get(FIELD_ID).in(listValueIds);
        
        List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot, root, 
                criteriaBuilder, valuePredicate);
        
        subqueryContext.subquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery));
    }

    private Predicate applyTextFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                     SubqueryContext subqueryContext, List<Predicate> basePredicates, 
                                     List<String> values) {
        List<Predicate> textPredicates = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String escapedValue = escapeQuery(value);
                String searchPattern = String.format(LIKE_PATTERN, escapedValue);
                textPredicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(subqueryContext.fieldValueRoot.get(FIELD_VALUE_TEXT)),
                                searchPattern
                        )
                );
            }
        }
        
        if (textPredicates.isEmpty()) {
            return predicate;
        }
        
        Predicate textPredicate = criteriaBuilder.or(textPredicates.toArray(new Predicate[0]));
        List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot, root, 
                criteriaBuilder, textPredicate);
        
        subqueryContext.subquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
        return criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery));
    }

    private List<Predicate> buildWherePredicates(List<Predicate> basePredicates, Root<ClientFieldValue> fieldValueRoot,
                                                Root<Client> root, CriteriaBuilder criteriaBuilder, 
                                                Predicate valuePredicate) {
        List<Predicate> wherePredicates = new ArrayList<>(basePredicates);
        wherePredicates.add(criteriaBuilder.equal(fieldValueRoot.get(FIELD_CLIENT), root));
        wherePredicates.add(valuePredicate);
        return wherePredicates;
    }

    private Predicate addDynamicFieldRangeFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                CriteriaBuilder criteriaBuilder, Long fieldId, List<String> values, 
                                                Long clientTypeId, boolean isFrom) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        
        SubqueryContext subqueryContext = createFieldValueSubquery(query);
        
        List<Predicate> basePredicates = new ArrayList<>();
        basePredicates.add(criteriaBuilder.equal(subqueryContext.fieldJoin.get(FIELD_ID), fieldId));
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(subqueryContext.clientTypeJoin.get(FIELD_ID), clientTypeId));
        }
        
        try {
            LocalDate date = LocalDate.parse(values.getFirst().trim());
            Predicate rangePredicate = isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(subqueryContext.fieldValueRoot.get(FIELD_VALUE_DATE), date) :
                    criteriaBuilder.lessThanOrEqualTo(subqueryContext.fieldValueRoot.get(FIELD_VALUE_DATE), date);
            
            List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot, root, 
                    criteriaBuilder, rangePredicate);
            
            subqueryContext.subquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery));
        } catch (DateTimeParseException e) {
            try {
                BigDecimal number = new BigDecimal(values.getFirst().trim());
                Predicate rangePredicate = isFrom ?
                        criteriaBuilder.greaterThanOrEqualTo(subqueryContext.fieldValueRoot.get(FIELD_VALUE_NUMBER), number) :
                        criteriaBuilder.lessThanOrEqualTo(subqueryContext.fieldValueRoot.get(FIELD_VALUE_NUMBER), number);
                
                List<Predicate> wherePredicates = buildWherePredicates(basePredicates, subqueryContext.fieldValueRoot, root, 
                        criteriaBuilder, rangePredicate);
                
                subqueryContext.subquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.exists(subqueryContext.subquery));
            } catch (NumberFormatException ex) {
                log.warn("Invalid range filter value format for field {}: {}", fieldId, values);
            }
        }
        
        return predicate;
    }

    private static class SubqueryContext {
        final Subquery<Long> subquery;
        final Root<ClientFieldValue> fieldValueRoot;
        final Join<ClientFieldValue, ClientTypeField> fieldJoin;
        final Join<ClientTypeField, ?> clientTypeJoin;

        SubqueryContext(Subquery<Long> subquery, Root<ClientFieldValue> fieldValueRoot,
                       Join<ClientFieldValue, ClientTypeField> fieldJoin,
                       Join<ClientTypeField, ?> clientTypeJoin) {
            this.subquery = subquery;
            this.fieldValueRoot = fieldValueRoot;
            this.fieldJoin = fieldJoin;
            this.clientTypeJoin = clientTypeJoin;
        }
    }

    private enum FieldType {
        BOOLEAN, LIST, TEXT, UNKNOWN
    }
}
