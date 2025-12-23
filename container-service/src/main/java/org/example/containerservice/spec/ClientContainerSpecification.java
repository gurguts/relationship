package org.example.containerservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.ClientContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientContainerSpecification implements Specification<ClientContainer> {

    private final String query;
    private final Map<String, List<String>> filterParams;
    private final List<Long> clientIds;

    public ClientContainerSpecification(String query, Map<String, List<String>> filterParams,
                                        List<Long> clientIds) {
        this.query = query;
        this.filterParams = filterParams;
        this.clientIds = clientIds;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<ClientContainer> root, CriteriaQuery<?> query,
                                 @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = applyFilters(criteriaBuilder.conjunction(), root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (StringUtils.hasText(this.query)) {
            List<Predicate> searchPredicates = new ArrayList<>();

            searchPredicates.add(
                    criteriaBuilder.like(
                            criteriaBuilder.toString(root.get("id")), String.format("%%%s%%", this.query.toLowerCase()))
            );

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

    private Predicate applyFilters(Predicate predicate, Root<ClientContainer> root, CriteriaBuilder criteriaBuilder) {
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            switch (key) {
                case "updatedAtFrom":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "updatedAt", true);
                    break;
                case "updatedAtTo":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "updatedAt", false);
                    break;
                case "quantityFrom":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "quantity", true);
                    break;
                case "quantityTo":
                    predicate = addNumericFilter(predicate, root, criteriaBuilder, values, "quantity", false);
                    break;
                case "container":
                    predicate = addIdContainerFilter(predicate, root, criteriaBuilder, values, "container");
                    break;
                case "user":
                    predicate = addIdFilter(predicate, root, criteriaBuilder, values, "user");
                    break;
                default:
                    break;
            }
        }

        return predicate;
    }


    private Predicate addIdFilter(Predicate predicate, Root<ClientContainer> root,
                                  CriteriaBuilder criteriaBuilder, List<String> values, String field) {
        if (!values.isEmpty()) {
            try {
                List<Long> ids = values.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

                predicate = criteriaBuilder.and(predicate, root.get(field).in(ids));
            } catch (NumberFormatException e) {
                throw new ContainerException("INVALID_FILTER", 
                    String.format("Incorrect ID format in filter %s: %s", field, values));
            }
        }
        return predicate;
    }

    private Predicate addIdContainerFilter(Predicate predicate, Root<ClientContainer> root,
                                           CriteriaBuilder criteriaBuilder, List<String> values, String field) {
        if (!values.isEmpty()) {
            try {
                List<Long> ids = values.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

                predicate = criteriaBuilder.and(predicate, root.get(field).get("id").in(ids));
            } catch (NumberFormatException e) {
                throw new ContainerException("INVALID_FILTER", 
                    String.format("Incorrect ID format in filter %s: %s", field, values));
            }
        }
        return predicate;
    }

    private Predicate addDateFilter(Predicate predicate, Root<ClientContainer> root, CriteriaBuilder criteriaBuilder,
                                    List<String> values, String field, boolean isFrom) {
        if (!values.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(values.getFirst());
                LocalDateTime dateTime = isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
                predicate = criteriaBuilder.and(predicate, isFrom ?
                        criteriaBuilder.greaterThanOrEqualTo(root.get(field), dateTime) :
                        criteriaBuilder.lessThanOrEqualTo(root.get(field), dateTime));
            } catch (DateTimeParseException e) {
                throw new ContainerException("INVALID_FILTER", 
                    String.format("Incorrect date format in filter %s: %s", field, values));
            }
        }
        return predicate;
    }

    private Predicate addNumericFilter(Predicate predicate, Root<ClientContainer> root, CriteriaBuilder criteriaBuilder,
                                       List<String> values, String field, boolean isFrom) {
        if (!values.isEmpty()) {
            try {
                Double value = Double.parseDouble(values.getFirst());
                predicate = criteriaBuilder.and(predicate, isFrom ?
                        criteriaBuilder.greaterThanOrEqualTo(root.get(field), value) :
                        criteriaBuilder.lessThanOrEqualTo(root.get(field), value));
            } catch (NumberFormatException e) {
                throw new ContainerException("INVALID_FILTER", 
                    String.format("Incorrect numeric format in filter %s: %s", field, values));
            }
        }
        return predicate;
    }
}