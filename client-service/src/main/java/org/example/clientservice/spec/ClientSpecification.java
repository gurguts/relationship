package org.example.clientservice.spec;

import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PhoneNumber;
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
            "createdAtFrom", "createdAtTo", "updatedAtFrom", "updatedAtTo",
            "business", "route", "region", "status", "source", "clientProduct");

    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> statusIds;
    private final List<Long> sourceIds;
    private final List<Long> routeIds;
    private final List<Long> regionIds;
    private final List<Long> businessIds;
    private final List<Long> clientProductIds;
    private final List<Long> excludedStatusIds;

    public ClientSpecification(String query, Map<String, List<String>> filterParams,
                               List<Long> statusIds, List<Long> sourceIds,
                               List<Long> routeIds, List<Long> regionIds,
                               List<Long> businessIds, List<Long> clientProductIds,
                               List<Long> excludedStatusIds) {
        this.query = query;
        this.filterParams = filterParams;
        this.statusIds = statusIds;
        this.sourceIds = sourceIds;
        this.routeIds = routeIds;
        this.regionIds = regionIds;
        this.businessIds = businessIds;
        this.clientProductIds = clientProductIds;
        this.excludedStatusIds = excludedStatusIds;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Client> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Predicate predicate = criteriaBuilder.conjunction();

        if (StringUtils.hasText(this.query)) {
            predicate = criteriaBuilder.and(predicate, createKeywordPredicate(root, query, criteriaBuilder));
        }

        predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(root.get("isActive")));

        if (excludedStatusIds != null && !excludedStatusIds.isEmpty()) {
            predicate = criteriaBuilder.and(predicate, createStatusPredicate(root, criteriaBuilder));
        }

        predicate = applyFilters(predicate, root, criteriaBuilder);

        return predicate;
    }

    private Predicate createKeywordPredicate(Root<Client> root, CriteriaQuery<?> query,
                                             CriteriaBuilder criteriaBuilder) {
        Predicate keywordPredicate = criteriaBuilder.or(
                criteriaBuilder.like(
                        criteriaBuilder.toString(root.get("id")), String.format("%%%s%%", escapeQuery(this.query))),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("company")), String.format("%%%s%%", escapeQuery(this.query))),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("person")), String.format("%%%s%%", escapeQuery(this.query))),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("location")), String.format("%%%s%%", escapeQuery(this.query))),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("volumeMonth")), String.format("%%%s%%", escapeQuery(this.query))),
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("comment")), String.format("%%%s%%", escapeQuery(this.query)))
        );

        keywordPredicate = addJoinPredicates(root, criteriaBuilder, keywordPredicate);
        keywordPredicate = addPhoneNumberPredicate(root, query, criteriaBuilder, keywordPredicate);

        return keywordPredicate;
    }

    private String escapeQuery(String query) {
        return query.toLowerCase().replace("%", "\\%").replace("_", "\\_");
    }

    private Predicate addJoinPredicates(Root<Client> root, CriteriaBuilder criteriaBuilder,
                                        Predicate keywordPredicate) {
        return criteriaBuilder.or(
                keywordPredicate,
                statusIds != null ? root.get("status").in(statusIds) : criteriaBuilder.conjunction(),
                sourceIds != null ? root.get("source").in(sourceIds) : criteriaBuilder.conjunction(),
                routeIds != null ? root.get("route").in(routeIds) : criteriaBuilder.conjunction(),
                regionIds != null ? root.get("region").in(regionIds) : criteriaBuilder.conjunction(),
                businessIds != null ? root.get("business").in(businessIds) : criteriaBuilder.conjunction(),
                clientProductIds != null ? root.get("clientProduct").in(clientProductIds) : criteriaBuilder.conjunction()
        );
    }

    private Predicate addPhoneNumberPredicate(Root<Client> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder,
                                              Predicate keywordPredicate) {
        Subquery<Long> phoneSubquery = query.subquery(Long.class);
        Root<PhoneNumber> phoneRoot = phoneSubquery.from(PhoneNumber.class);
        phoneSubquery.select(phoneRoot.get("id"));
        phoneSubquery.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(phoneRoot.get("client"), root),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(phoneRoot.get("number")),
                                String.format("%%%s%%", escapeQuery(this.query)))
                )
        );

        return criteriaBuilder.or(
                keywordPredicate,
                criteriaBuilder.exists(phoneSubquery)
        );
    }

    private Predicate createStatusPredicate(Root<Client> root, CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder.or(
                criteriaBuilder.isNull(root.get("status")),
                criteriaBuilder.not(root.get("status").in(excludedStatusIds))
        );
    }

    private Predicate applyFilters(Predicate predicate, Root<Client> root, CriteriaBuilder criteriaBuilder) {
        if (filterParams == null || filterParams.isEmpty()) {
            return predicate;
        }

        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (!VALID_FILTER_KEYS.contains(key)) {
                continue;
            }

            switch (key) {
                case "createdAtFrom" -> predicate =
                        addDateFilter(predicate, root, criteriaBuilder, values, "createdAt", true);
                case "createdAtTo" -> predicate =
                        addDateFilter(predicate, root, criteriaBuilder, values, "createdAt", false);
                case "updatedAtFrom" -> predicate =
                        addDateFilter(predicate, root, criteriaBuilder, values, "updatedAt", true);
                case "updatedAtTo" -> predicate =
                        addDateFilter(predicate, root, criteriaBuilder, values, "updatedAt", false);
                case "business" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "business");
                case "route" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "route");
                case "region" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "region");
                case "status" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "status");
                case "source" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "source");
                case "clientProduct" -> predicate = addIdFilter(predicate, root, criteriaBuilder, values, "clientProduct");
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

}