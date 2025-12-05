package org.example.clientservice.spec;

import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientSpecification implements Specification<Client> {
    private static final Set<String> VALID_FILTER_KEYS = Set.of(
            "createdAtFrom", "createdAtTo", "updatedAtFrom", "updatedAtTo", "source", "showInactive");

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

        if (StringUtils.hasText(this.query)) {
            predicate = criteriaBuilder.and(predicate, createKeywordPredicate(root, query, criteriaBuilder));
        }

        // Фильтр по isActive: если showInactive установлен, показываем только неактивных, иначе только активных
        boolean showInactive = filterParams != null && filterParams.containsKey("showInactive") 
                && filterParams.get("showInactive") != null 
                && !filterParams.get("showInactive").isEmpty()
                && "true".equals(filterParams.get("showInactive").get(0));
        
        if (showInactive) {
            // Показываем только неактивных клиентов
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.isFalse(root.get("isActive")));
        } else {
            // По умолчанию показываем только активных клиентов
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(root.get("isActive")));
        }

        // Получаем или создаем JOIN для clientType
        Join<?, ?> clientTypeJoin = null;
        for (Join<?, ?> join : root.getJoins()) {
            if ("clientType".equals(join.getAttribute().getName())) {
                clientTypeJoin = join;
                break;
            }
        }
        if (clientTypeJoin == null) {
            clientTypeJoin = root.join("clientType", JoinType.INNER);
        }

        // Фильтр по доступным типам клиентов (для прав доступа)
        if (allowedClientTypeIds != null && !allowedClientTypeIds.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, 
                clientTypeJoin.get("id").in(allowedClientTypeIds));
        }

        // Фильтр по конкретному типу клиента
        if (clientTypeId != null) {
            predicate = criteriaBuilder.and(predicate, 
                criteriaBuilder.equal(clientTypeJoin.get("id"), clientTypeId));
        }

        predicate = applyFilters(predicate, root, query, criteriaBuilder);

        return predicate;
    }

    private Predicate createKeywordPredicate(Root<Client> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
        Predicate keywordPredicate = criteriaBuilder.or(
                criteriaBuilder.like(
                        criteriaBuilder.toString(root.get("id")), String.format("%%%s%%", escapeQuery(this.query))),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("company")), String.format("%%%s%%", escapeQuery(this.query)))
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
        return criteriaBuilder.or(
                keywordPredicate,
                sourceIds != null ? root.get("source").in(sourceIds) : criteriaBuilder.conjunction()
        );
    }

    private Predicate addDynamicFieldSearchPredicate(Root<Client> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder,
                                                     Predicate keywordPredicate) {
        // Поиск по динамическим полям (ClientFieldValue)
        // Ищем по valueText (для TEXT и PHONE полей) и по valueList.value (для LIST полей)
        
        Subquery<Long> fieldValueSubquery = query.subquery(Long.class);
        Root<ClientFieldValue> fieldValueRoot = fieldValueSubquery.from(ClientFieldValue.class);
        Join<ClientFieldValue, ClientTypeField> fieldJoin = fieldValueRoot.join("field");
        Join<ClientTypeField, ?> clientTypeJoin = fieldJoin.join("clientType");
        
        fieldValueSubquery.select(fieldValueRoot.get("client").get("id"));
        
        // Если указан clientTypeId, фильтруем также по типу клиента
        List<Predicate> basePredicates = new java.util.ArrayList<>();
        basePredicates.add(criteriaBuilder.equal(fieldValueRoot.get("client"), root));
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(clientTypeJoin.get("id"), clientTypeId));
        }
        
        // Ищем только в searchable полях
        basePredicates.add(criteriaBuilder.isTrue(fieldJoin.get("isSearchable")));
        
        String escapedQuery = escapeQuery(this.query);
        String searchPattern = String.format("%%%s%%", escapedQuery);
        
        // Поиск по valueText (для TEXT и PHONE полей)
        Predicate textSearchPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(fieldValueRoot.get("valueText")),
                searchPattern
        );
        
        // Поиск по valueList.value (для LIST полей - по названию значения списка)
        Join<ClientFieldValue, ?> valueListJoin = fieldValueRoot.join("valueList", jakarta.persistence.criteria.JoinType.LEFT);
        Predicate listValueSearchPredicate = criteriaBuilder.like(
                criteriaBuilder.lower(valueListJoin.get("value")),
                searchPattern
        );
        
        // Объединяем поиск по тексту и по значениям списка
        Predicate dynamicFieldPredicate = criteriaBuilder.or(textSearchPredicate, listValueSearchPredicate);
        
        basePredicates.add(dynamicFieldPredicate);
        
        fieldValueSubquery.where(criteriaBuilder.and(basePredicates.toArray(new Predicate[0])));
        
        return criteriaBuilder.or(
                keywordPredicate,
                criteriaBuilder.exists(fieldValueSubquery)
        );
    }

    private Predicate applyFilters(Predicate predicate, Root<Client> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (filterParams == null || filterParams.isEmpty()) {
            return predicate;
        }

        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            // Обрабатываем стандартные поля
            if (VALID_FILTER_KEYS.contains(key)) {
                switch (key) {
                    case "createdAtFrom" -> predicate =
                            addDateFilter(predicate, root, criteriaBuilder, values, "createdAt", true);
                    case "createdAtTo" -> predicate =
                            addDateFilter(predicate, root, criteriaBuilder, values, "createdAt", false);
                    case "updatedAtFrom" -> predicate =
                            addDateFilter(predicate, root, criteriaBuilder, values, "updatedAt", true);
                    case "updatedAtTo" -> predicate =
                            addDateFilter(predicate, root, criteriaBuilder, values, "updatedAt", false);
                    case "source" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "source");
                    case "showInactive" -> {
                        // Логика showInactive обрабатывается в toPredicate, здесь просто пропускаем
                        // чтобы не обрабатывать этот ключ как динамическое поле
                    }
                }
            } else if (!key.endsWith("From") && !key.endsWith("To") && !key.equals("showInactive") && !key.equals("clientTypeId")) {
                // Обрабатываем динамические поля (не стандартные и не диапазоны)
                // clientTypeId обрабатывается отдельно в toPredicate, поэтому пропускаем его здесь
                predicate = addDynamicFieldFilter(predicate, root, query, criteriaBuilder, key, values, clientTypeId);
            }
        }

        return predicate;
    }

    private Predicate addIdFilter(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder,
                                  List<String> values, String field) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }
        try {
            List<Long> ids = values.stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            predicate = criteriaBuilder.and(predicate, root.get(field).in(ids));
        } catch (NumberFormatException e) {
            throw new ClientException(String.format("Incorrect ID format in filter %s: %s", field, values));
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
            throw new ClientException(String.format("Incorrect date format in filter %s: %s", field, values));
        }
        return predicate;
    }

    private Predicate addDynamicFieldFilter(Predicate predicate, Root<Client> root, CriteriaQuery<?> query,
                                            CriteriaBuilder criteriaBuilder, String fieldName, List<String> values, Long clientTypeId) {
        if (values == null || values.isEmpty()) {
            return predicate;
        }

        // Для динамических полей фильтруем через ClientFieldValue
        // fieldName - это имя поля (например, "region" или "Region")
        // values - список ID значений списка (для LIST полей) или текстовые значения (для TEXT полей)
        
        Subquery<Long> fieldValueSubquery = query.subquery(Long.class);
        Root<ClientFieldValue> fieldValueRoot = fieldValueSubquery.from(ClientFieldValue.class);
        Join<ClientFieldValue, ClientTypeField> fieldJoin = fieldValueRoot.join("field");
        Join<ClientTypeField, ?> clientTypeJoin = fieldJoin.join("clientType");
        
        fieldValueSubquery.select(fieldValueRoot.get("client").get("id"));
        
        // Фильтруем по имени поля (case-insensitive)
        Predicate fieldNamePredicate = criteriaBuilder.equal(
            criteriaBuilder.lower(fieldJoin.get("fieldName")), 
            fieldName.toLowerCase()
        );
        
        // Если указан clientTypeId, фильтруем также по типу клиента
        List<Predicate> basePredicates = new java.util.ArrayList<>();
        basePredicates.add(fieldNamePredicate);
        if (clientTypeId != null) {
            basePredicates.add(criteriaBuilder.equal(clientTypeJoin.get("id"), clientTypeId));
        }
        
        // Определяем тип поля по значениям
        // Проверяем, является ли значение булевым ("true" или "false")
        boolean isBooleanField = false;
        List<Boolean> booleanValues = new java.util.ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String trimmedValue = value.trim().toLowerCase();
                if ("true".equals(trimmedValue) || "false".equals(trimmedValue)) {
                    isBooleanField = true;
                    booleanValues.add("true".equals(trimmedValue));
                } else {
                    isBooleanField = false;
                    break;
                }
            }
        }
        
        if (isBooleanField && !booleanValues.isEmpty()) {
            // Для BOOLEAN полей фильтруем по valueBoolean
            Predicate booleanPredicate = fieldValueRoot.get("valueBoolean").in(booleanValues);
            
            List<Predicate> wherePredicates = new java.util.ArrayList<>(basePredicates);
            wherePredicates.add(criteriaBuilder.equal(fieldValueRoot.get("client"), root));
            wherePredicates.add(booleanPredicate);
            
            fieldValueSubquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
            
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.exists(fieldValueSubquery));
        } else {
            // Пытаемся распарсить значения как Long (для LIST полей)
            boolean isListField = true;
            List<Long> listValueIds = new java.util.ArrayList<>();
            try {
                for (String value : values) {
                    if (value != null && !value.trim().isEmpty()) {
                        listValueIds.add(Long.parseLong(value.trim()));
                    }
                }
                if (listValueIds.isEmpty()) {
                    isListField = false;
                }
            } catch (NumberFormatException e) {
                // Если не удалось распарсить как Long, это TEXT поле
                isListField = false;
            }
            
            if (isListField && !listValueIds.isEmpty()) {
                // Для LIST полей фильтруем по valueList.id
                Join<ClientFieldValue, ?> valueListJoin = fieldValueRoot.join("valueList", jakarta.persistence.criteria.JoinType.LEFT);
                Predicate valuePredicate = valueListJoin.get("id").in(listValueIds);
                
                List<Predicate> wherePredicates = new java.util.ArrayList<>(basePredicates);
                wherePredicates.add(criteriaBuilder.equal(fieldValueRoot.get("client"), root));
                wherePredicates.add(valuePredicate);
                
                fieldValueSubquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
                
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.exists(fieldValueSubquery));
            } else {
                // Для TEXT полей фильтруем по valueText
                List<Predicate> textPredicates = new java.util.ArrayList<>();
                for (String value : values) {
                    if (value != null && !value.trim().isEmpty()) {
                        textPredicates.add(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(fieldValueRoot.get("valueText")),
                                        String.format("%%%s%%", value.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_"))
                                )
                        );
                    }
                }
                
                if (!textPredicates.isEmpty()) {
                    Predicate textPredicate = criteriaBuilder.or(textPredicates.toArray(new Predicate[0]));
                    
                    List<Predicate> wherePredicates = new java.util.ArrayList<>(basePredicates);
                    wherePredicates.add(criteriaBuilder.equal(fieldValueRoot.get("client"), root));
                    wherePredicates.add(textPredicate);
                    
                    fieldValueSubquery.where(criteriaBuilder.and(wherePredicates.toArray(new Predicate[0])));
                    
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.exists(fieldValueSubquery));
                }
            }
        }
        
        return predicate;
    }

}