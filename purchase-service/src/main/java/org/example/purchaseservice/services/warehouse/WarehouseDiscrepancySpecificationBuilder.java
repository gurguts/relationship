package org.example.purchaseservice.services.warehouse;

import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WarehouseDiscrepancySpecificationBuilder {
    
    public Specification<WarehouseDiscrepancy> buildSpecification(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo) {
        
        return (root, _, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (driverId != null) {
                predicates.add(criteriaBuilder.equal(root.get("driverId"), driverId));
            }
            if (productId != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), productId));
            }
            if (warehouseId != null) {
                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
            }
            if (type != null && !type.trim().isEmpty()) {
                WarehouseDiscrepancy.DiscrepancyType discrepancyType = 
                        WarehouseDiscrepancy.DiscrepancyType.valueOf(type.toUpperCase());
                predicates.add(criteriaBuilder.equal(root.get("type"), discrepancyType));
            }
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("receiptDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("receiptDate"), dateTo));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
