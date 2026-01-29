package org.example.purchaseservice.services.warehouse;

import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ProductTransferSpecificationBuilder {
    
    private static final String DEFAULT_SORT_PROPERTY = "transferDate";
    private static final String SORT_DIRECTION_DESC = "desc";
    private static final Set<String> VALID_SORT_PROPERTIES = Set.of(
            "id", "warehouseId", "fromProductId", "toProductId", "quantity",
            "unitPriceEur", "totalCostEur", "transferDate", "userId", "createdAt", "updatedAt"
    );
    
    public Specification<ProductTransfer> buildTransferSpecification(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) {
        
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transferDate"), dateFrom));
            }
            
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transferDate"), dateTo));
            }
            
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            
            if (fromProductId != null) {
                predicates.add(cb.equal(root.get("fromProductId"), fromProductId));
            }
            
            if (toProductId != null) {
                predicates.add(cb.equal(root.get("toProductId"), toProductId));
            }
            
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            
            if (reasonId != null) {
                predicates.add(cb.equal(root.get("reason").get("id"), reasonId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public String validateAndGetSortProperty(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return DEFAULT_SORT_PROPERTY;
        }
        if (!VALID_SORT_PROPERTIES.contains(sortBy)) {
            throw new org.example.purchaseservice.exceptions.PurchaseException("INVALID_SORT_PROPERTY",
                    String.format("Invalid sort property: %s. Valid properties: %s",
                            sortBy, String.join(", ", VALID_SORT_PROPERTIES)));
        }
        return sortBy;
    }
    
    public Sort.Direction parseSortDirection(String sortDirection) {
        return SORT_DIRECTION_DESC.equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }
    
    public Sort createDefaultSort() {
        return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY);
    }
}
