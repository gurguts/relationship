package org.example.clientservice.spec;

import jakarta.persistence.criteria.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.client.Client;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.example.clientservice.spec.ClientFilterValueParser.*;
import static org.example.clientservice.spec.ClientDynamicFieldFilterBuilder.*;

@Slf4j
public class ClientSpecification implements Specification<Client> {
    private static final String FIELD_PREFIX = "field_";
    private static final String SUFFIX_FROM = "From";
    private static final String SUFFIX_TO = "To";
    
    private static final String FIELD_CLIENT_TYPE = "clientType";
    private static final String FIELD_IS_ACTIVE = "isActive";
    private static final String FIELD_ID = "id";
    private static final String FIELD_COMPANY = "company";
    private static final String FIELD_SOURCE_ID = "sourceId";
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
    public Predicate toPredicate(@NonNull Root<Client> root, CriteriaQuery<?> query, @NonNull CriteriaBuilder criteriaBuilder) {
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
        String searchPattern = createSearchPattern(this.query);
        
        Predicate keywordPredicate = criteriaBuilder.or(
                criteriaBuilder.like(
                        criteriaBuilder.toString(root.get(FIELD_ID)), searchPattern),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(FIELD_COMPANY)), searchPattern)
        );

        keywordPredicate = addJoinPredicates(root, criteriaBuilder, keywordPredicate);
        keywordPredicate = addDynamicFieldSearchPredicate(root, query, criteriaBuilder, keywordPredicate, 
                this.query, clientTypeId);

        return keywordPredicate;
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


    private Predicate addSourceIdFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                       List<String> values) {
        List<Long> ids = parseSourceIds(values);
        if (!ids.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, root.get(FIELD_SOURCE_ID).in(ids));
        }
        return predicate;
    }

    private Predicate addDateFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                    List<String> values, String field, boolean isFrom) {
        LocalDateTime dateTime = parseDateFilter(values, isFrom);
        if (dateTime != null) {
            predicate = criteriaBuilder.and(predicate, isFrom ?
                    criteriaBuilder.greaterThanOrEqualTo(root.get(field), dateTime) :
                    criteriaBuilder.lessThanOrEqualTo(root.get(field), dateTime));
        }
        return predicate;
    }

    private Predicate addDynamicFieldFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                            CriteriaBuilder criteriaBuilder, String fieldName, List<String> values, 
                                            Long clientTypeId) {
        return ClientDynamicFieldFilterBuilder.addDynamicFieldFilter(
                predicate, root, query, criteriaBuilder, fieldName, values, clientTypeId);
    }

    private Predicate addDynamicFieldFilterById(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                CriteriaBuilder criteriaBuilder, Long fieldId, List<String> values, 
                                                Long clientTypeId) {
        return ClientDynamicFieldFilterBuilder.addDynamicFieldFilterById(
                predicate, root, query, criteriaBuilder, fieldId, values, clientTypeId);
    }

    private Predicate addDynamicFieldRangeFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                                CriteriaBuilder criteriaBuilder, Long fieldId, List<String> values, 
                                                Long clientTypeId, boolean isFrom) {
        return ClientDynamicFieldFilterBuilder.addDynamicFieldRangeFilter(
                predicate, root, query, criteriaBuilder, fieldId, values, clientTypeId, isFrom);
    }
}
