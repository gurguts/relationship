package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StockVehicleSpecification implements Specification<Vehicle> {

    private static final String SHIPMENT_DATE = "shipmentDate";
    private static final String VEHICLE_NUMBER = "vehicleNumber";
    private static final String DESCRIPTION = "description";
    private static final String MANAGER_ID = "managerId";

    private final String query;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final List<Long> managerIds;

    public StockVehicleSpecification(String query, LocalDate fromDate, LocalDate toDate, List<Long> managerIds) {
        this.query = query;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.managerIds = managerIds != null ? managerIds : new ArrayList<>();
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Vehicle> root, CriteriaQuery<?> criteriaQuery,
                                 @NonNull CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        if (fromDate != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(SHIPMENT_DATE), fromDate));
        }
        if (toDate != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(SHIPMENT_DATE), toDate));
        }
        if (!managerIds.isEmpty()) {
            predicates.add(root.get(MANAGER_ID).in(managerIds));
        }
        if (StringUtils.hasText(query)) {
            String pattern = "%" + query.toLowerCase() + "%";
            Predicate vehicleNumberLike = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get(VEHICLE_NUMBER), "")), pattern);
            Predicate descriptionLike = criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get(DESCRIPTION), "")), pattern);
            predicates.add(criteriaBuilder.or(vehicleNumberLike, descriptionLike));
        }

        if (criteriaQuery.getResultType() != Long.class && criteriaQuery.getResultType() != long.class) {
            var shipmentDatePath = root.get(SHIPMENT_DATE);
            var nullRank = criteriaBuilder.selectCase()
                    .when(criteriaBuilder.isNull(shipmentDatePath), 0)
                    .otherwise(1);
            criteriaQuery.orderBy(
                    criteriaBuilder.asc(nullRank),
                    criteriaBuilder.desc(shipmentDatePath));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
