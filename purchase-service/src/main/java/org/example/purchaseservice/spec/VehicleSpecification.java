package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehicleSpecification implements Specification<Vehicle> {
    private final String query;
    private final Map<String, List<String>> filterParams;

    public VehicleSpecification(String query, Map<String, List<String>> filterParams) {
        this.query = query;
        this.filterParams = filterParams != null ? filterParams : Map.of();
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Vehicle> root, CriteriaQuery<?> criteriaQuery,
                                 @NotNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate filterPredicate = applyFilters(criteriaBuilder.conjunction(), root, criteriaBuilder);
        predicates.add(filterPredicate);

        if (StringUtils.hasText(this.query)) {
            List<Predicate> searchPredicates = new ArrayList<>();
            String searchTerm = "%" + this.query.toLowerCase() + "%";

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.toString(root.get("id"))), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("vehicleNumber")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("invoiceUa")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("invoiceEu")), searchTerm));

            Join<Vehicle, VehicleSender> senderJoin = root.join("sender", JoinType.LEFT);
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(senderJoin.get("name")), searchTerm));

            Join<Vehicle, VehicleReceiver> receiverJoin = root.join("receiver", JoinType.LEFT);
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(receiverJoin.get("name")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("destinationCountry")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("destinationPlace")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("product")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("productQuantity")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("declarationNumber")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("terminal")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("driverFullName")), searchTerm));

            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchTerm));

            Join<Vehicle, Carrier> carrierJoin = root.join("carrier", JoinType.LEFT);
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(carrierJoin.get("companyName")), searchTerm));

            Predicate searchPredicate = criteriaBuilder.or(
                    searchPredicates.toArray(new Predicate[0]));
            predicates.add(searchPredicate);
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate applyFilters(Predicate predicate, Root<Vehicle> root, CriteriaBuilder criteriaBuilder) {
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                continue;
            }

            switch (key) {
                case "shipmentDateFrom":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "shipmentDate", true);
                    break;
                case "shipmentDateTo":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "shipmentDate", false);
                    break;
                case "customsDateFrom":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "customsDate", true);
                    break;
                case "customsDateTo":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "customsDate", false);
                    break;
                case "customsClearanceDateFrom":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "customsClearanceDate", true);
                    break;
                case "customsClearanceDateTo":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "customsClearanceDate", false);
                    break;
                case "unloadingDateFrom":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "unloadingDate", true);
                    break;
                case "unloadingDateTo":
                    predicate = addDateFilter(predicate, root, criteriaBuilder, values, "unloadingDate", false);
                    break;
                case "isOurVehicle":
                    if (values.get(0).equalsIgnoreCase("true")) {
                        predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(root.get("isOurVehicle")));
                    } else if (values.get(0).equalsIgnoreCase("false")) {
                        predicate = criteriaBuilder.and(predicate, criteriaBuilder.isFalse(root.get("isOurVehicle")));
                    }
                    break;
                default:
                    break;
            }
        }

        return predicate;
    }

    private Predicate addDateFilter(Predicate predicate, Root<Vehicle> root, CriteriaBuilder criteriaBuilder,
                                    List<String> values, String field, boolean isFrom) {
        if (!values.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(values.get(0));
                if (isFrom) {
                    predicate = criteriaBuilder.and(predicate,
                            criteriaBuilder.greaterThanOrEqualTo(root.get(field), date));
                } else {
                    predicate = criteriaBuilder.and(predicate,
                            criteriaBuilder.lessThanOrEqualTo(root.get(field), date));
                }
            } catch (Exception e) {
            }
        }
        return predicate;
    }
}

