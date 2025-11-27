package org.example.purchaseservice.restControllers.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.WarehouseDiscrepancyDTO;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.repositories.WarehouseDiscrepancyRepository;
import org.example.purchaseservice.services.warehouse.WarehouseDiscrepancyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse/discrepancies")
@RequiredArgsConstructor
public class WarehouseDiscrepancyController {
    
    private final WarehouseDiscrepancyRepository discrepancyRepository;
    private final WarehouseDiscrepancyService discrepancyService;
    
    /**
     * Get all discrepancies with pagination, filtering and sorting
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping
    public ResponseEntity<PageResponse<WarehouseDiscrepancyDTO>> getDiscrepancies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        log.info("Fetching discrepancies: page={}, size={}, sort={}, direction={}", page, size, sort, direction);
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Specification<WarehouseDiscrepancy> spec = (root, query, criteriaBuilder) -> {
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
            if (type != null && !type.isEmpty()) {
                try {
                    WarehouseDiscrepancy.DiscrepancyType discrepancyType = 
                            WarehouseDiscrepancy.DiscrepancyType.valueOf(type.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("type"), discrepancyType));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid discrepancy type: {}", type);
                }
            }
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("receiptDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("receiptDate"), dateTo));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<WarehouseDiscrepancy> discrepancyPage = discrepancyRepository.findAll(spec, pageRequest);
        
        List<WarehouseDiscrepancyDTO> dtos = discrepancyPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        PageResponse<WarehouseDiscrepancyDTO> response = new PageResponse<>(
                discrepancyPage.getNumber(),
                discrepancyPage.getSize(),
                (int) discrepancyPage.getTotalElements(),
                discrepancyPage.getTotalPages(),
                dtos
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get discrepancy statistics with optional filters
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/statistics")
    public ResponseEntity<DiscrepancyStatistics> getStatistics(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        
        // Build specification for filtering
        Specification<WarehouseDiscrepancy> spec = buildSpecification(type, dateFrom, dateTo);
        
        // Get filtered discrepancies
        List<WarehouseDiscrepancy> filteredDiscrepancies = discrepancyRepository.findAll(spec);
        
        // Calculate statistics from filtered data
        BigDecimal totalLosses = filteredDiscrepancies.stream()
                .filter(d -> d.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS)
                .map(WarehouseDiscrepancy::getDiscrepancyValueEur)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs(); // Make positive for display
        
        BigDecimal totalGains = filteredDiscrepancies.stream()
                .filter(d -> d.getType() == WarehouseDiscrepancy.DiscrepancyType.GAIN)
                .map(WarehouseDiscrepancy::getDiscrepancyValueEur)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long lossCount = filteredDiscrepancies.stream()
                .filter(d -> d.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS)
                .count();
        
        long gainCount = filteredDiscrepancies.stream()
                .filter(d -> d.getType() == WarehouseDiscrepancy.DiscrepancyType.GAIN)
                .count();
        
        DiscrepancyStatistics stats = new DiscrepancyStatistics();
        stats.setTotalLossesValue(totalLosses);
        stats.setTotalGainsValue(totalGains);
        stats.setLossCount(lossCount);
        stats.setGainCount(gainCount);
        stats.setNetValue(totalGains.add(totalLosses.negate())); // totalLosses is positive, so subtract it
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Build specification for filtering discrepancies
     */
    private Specification<WarehouseDiscrepancy> buildSpecification(String type, LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (type != null && !type.isEmpty()) {
                try {
                    WarehouseDiscrepancy.DiscrepancyType discrepancyType = 
                            WarehouseDiscrepancy.DiscrepancyType.valueOf(type.toUpperCase());
                    predicates.add(cb.equal(root.get("type"), discrepancyType));
                } catch (IllegalArgumentException e) {
                    // Invalid type, ignore
                }
            }
            
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), dateFrom));
            }
            
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receiptDate"), dateTo));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Get discrepancy by ID
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDiscrepancyDTO> getDiscrepancy(@PathVariable Long id) {
        return discrepancyRepository.findById(id)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Export discrepancies to Excel with filters
     */
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        log.info("Exporting discrepancies to Excel with filters: driverId={}, productId={}, warehouseId={}, type={}, dateFrom={}, dateTo={}",
                driverId, productId, warehouseId, type, dateFrom, dateTo);
        
        try {
            byte[] excelData = discrepancyService.exportToExcel(
                    driverId, productId, warehouseId, type, dateFrom, dateTo
            );
            
            String filename = "vtrati_ta_pridbanna_" + 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(filename)
                    .build()
            );
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (IOException e) {
            log.error("Error exporting discrepancies to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private WarehouseDiscrepancyDTO mapToDTO(WarehouseDiscrepancy discrepancy) {
        return WarehouseDiscrepancyDTO.builder()
                .id(discrepancy.getId())
                .warehouseReceiptId(discrepancy.getWarehouseReceiptId())
                .driverId(discrepancy.getDriverId())
                .productId(discrepancy.getProductId())
                .warehouseId(discrepancy.getWarehouseId())
                .receiptDate(discrepancy.getReceiptDate())
                .purchasedQuantity(discrepancy.getPurchasedQuantity())
                .receivedQuantity(discrepancy.getReceivedQuantity())
                .discrepancyQuantity(discrepancy.getDiscrepancyQuantity())
                .unitPriceEur(discrepancy.getUnitPriceEur())
                .discrepancyValueEur(discrepancy.getDiscrepancyValueEur())
                .type(discrepancy.getType().name())
                .comment(discrepancy.getComment())
                .createdByUserId(discrepancy.getCreatedByUserId())
                .createdAt(discrepancy.getCreatedAt())
                .build();
    }
    
    @lombok.Data
    public static class DiscrepancyStatistics {
        private BigDecimal totalLossesValue;
        private BigDecimal totalGainsValue;
        private BigDecimal netValue;
        private long lossCount;
        private long gainCount;
    }
}

