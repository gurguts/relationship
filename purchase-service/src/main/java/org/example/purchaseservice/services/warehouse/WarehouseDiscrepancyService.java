package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.repositories.WarehouseDiscrepancyRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseDiscrepancyService {
    
    private final WarehouseDiscrepancyRepository discrepancyRepository;
    private final org.example.purchaseservice.repositories.ProductRepository productRepository;
    private final org.example.purchaseservice.repositories.WarehouseRepository warehouseRepository;
    
    /**
     * Create discrepancy record when received quantity differs from purchased quantity
     */
    @Transactional
    public WarehouseDiscrepancy createDiscrepancy(
            Long warehouseReceiptId,
            Long driverId,
            Long productId,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal purchasedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal unitPriceUah,
            Long createdByUserId,
            String comment) {
        
        BigDecimal discrepancyQuantity = receivedQuantity.subtract(purchasedQuantity);
        
        // Don't create record if there's no discrepancy
        if (discrepancyQuantity.compareTo(BigDecimal.ZERO) == 0) {
            log.info("No discrepancy detected for product {}, skipping record creation", productId);
            return null;
        }
        
        BigDecimal discrepancyValue = discrepancyQuantity.multiply(unitPriceUah)
                .setScale(2, RoundingMode.HALF_UP);
        
        WarehouseDiscrepancy.DiscrepancyType type = discrepancyQuantity.compareTo(BigDecimal.ZERO) > 0 
                ? WarehouseDiscrepancy.DiscrepancyType.GAIN 
                : WarehouseDiscrepancy.DiscrepancyType.LOSS;
        
        WarehouseDiscrepancy discrepancy = WarehouseDiscrepancy.builder()
                .warehouseReceiptId(warehouseReceiptId)
                .driverId(driverId)
                .productId(productId)
                .warehouseId(warehouseId)
                .receiptDate(receiptDate)
                .purchasedQuantity(purchasedQuantity)
                .receivedQuantity(receivedQuantity)
                .discrepancyQuantity(discrepancyQuantity)
                .unitPriceUah(unitPriceUah)
                .discrepancyValueUah(discrepancyValue)
                .type(type)
                .comment(comment)
                .createdByUserId(createdByUserId)
                .build();
        
        WarehouseDiscrepancy saved = discrepancyRepository.save(discrepancy);
        
        log.info("Discrepancy created: type={}, product={}, driver={}, quantity={}, value={} UAH", 
                type, productId, driverId, discrepancyQuantity, discrepancyValue);
        
        return saved;
    }
    
    /**
     * Create discrepancy from driver balance and received quantity
     */
    @Transactional
    public WarehouseDiscrepancy createFromDriverBalance(
            Long warehouseReceiptId,
            DriverProductBalance driverBalance,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal receivedQuantity,
            Long createdByUserId,
            String comment) {
        
        return createDiscrepancy(
                warehouseReceiptId,
                driverBalance.getDriverId(),
                driverBalance.getProductId(),
                warehouseId,
                receiptDate,
                driverBalance.getQuantity(), // purchased
                receivedQuantity,            // received
                driverBalance.getAveragePriceUah(),
                createdByUserId,
                comment
        );
    }
    
    /**
     * Get all discrepancies for specific warehouse receipt
     */
    public List<WarehouseDiscrepancy> getByWarehouseReceiptId(Long warehouseReceiptId) {
        return discrepancyRepository.findByWarehouseReceiptId(warehouseReceiptId);
    }
    
    /**
     * Get all discrepancies by driver
     */
    public List<WarehouseDiscrepancy> getByDriverId(Long driverId) {
        return discrepancyRepository.findByDriverId(driverId);
    }
    
    /**
     * Get all discrepancies by product
     */
    public List<WarehouseDiscrepancy> getByProductId(Long productId) {
        return discrepancyRepository.findByProductId(productId);
    }
    
    /**
     * Get all discrepancies by warehouse
     */
    public List<WarehouseDiscrepancy> getByWarehouseId(Long warehouseId) {
        return discrepancyRepository.findByWarehouseId(warehouseId);
    }
    
    /**
     * Get all discrepancies by type
     */
    public List<WarehouseDiscrepancy> getByType(WarehouseDiscrepancy.DiscrepancyType type) {
        return discrepancyRepository.findByType(type);
    }
    
    /**
     * Get discrepancies within date range
     */
    public List<WarehouseDiscrepancy> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return discrepancyRepository.findByReceiptDateBetween(startDate, endDate);
    }
    
    /**
     * Get total losses value
     */
    public BigDecimal getTotalLossesValue() {
        return discrepancyRepository.getTotalLossesValue();
    }
    
    /**
     * Get total gains value
     */
    public BigDecimal getTotalGainsValue() {
        return discrepancyRepository.getTotalGainsValue();
    }
    
    /**
     * Export discrepancies to Excel with filters
     */
    public byte[] exportToExcel(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo) throws IOException {
        
        // Build specification with filters
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
        
        List<WarehouseDiscrepancy> discrepancies = discrepancyRepository.findAll(spec);
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Втрати та придбання");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            
            // Create loss style (red text)
            CellStyle lossStyle = workbook.createCellStyle();
            lossStyle.cloneStyleFrom(dataStyle);
            Font lossFont = workbook.createFont();
            lossFont.setColor(IndexedColors.RED.getIndex());
            lossStyle.setFont(lossFont);
            
            // Create gain style (green text)
            CellStyle gainStyle = workbook.createCellStyle();
            gainStyle.cloneStyleFrom(dataStyle);
            Font gainFont = workbook.createFont();
            gainFont.setColor(IndexedColors.GREEN.getIndex());
            gainStyle.setFont(gainFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "№", "Дата", "Водій ID", "Товар", "Склад",
                "Закуплено (кг)", "Прийнято (кг)", "Різниця (кг)",
                "Ціна за кг (грн)", "Вартість різниці (грн)", "Тип", "Коментар"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            int rowNum = 1;
            
            for (WarehouseDiscrepancy discrepancy : discrepancies) {
                Row row = sheet.createRow(rowNum);
                
                boolean isLoss = discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS;
                CellStyle typeStyle = isLoss ? lossStyle : gainStyle;
                
                // Get entity names
                String productName = productRepository.findById(discrepancy.getProductId())
                        .map(p -> p.getName())
                        .orElse("Невідомо");
                String warehouseName = warehouseRepository.findById(discrepancy.getWarehouseId())
                        .map(w -> w.getName())
                        .orElse("Невідомо");
                
                // Column 0: Row number
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyle);
                
                // Column 1: Date
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(discrepancy.getReceiptDate().format(dateFormatter));
                cell1.setCellStyle(dataStyle);
                
                // Column 2: Driver ID (users are in another service)
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(discrepancy.getDriverId());
                cell2.setCellStyle(dataStyle);
                
                // Column 3: Product name
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(productName);
                cell3.setCellStyle(dataStyle);
                
                // Column 4: Warehouse name
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(warehouseName);
                cell4.setCellStyle(dataStyle);
                
                // Column 5: Purchased quantity
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(discrepancy.getPurchasedQuantity().doubleValue());
                cell5.setCellStyle(dataStyle);
                
                // Column 6: Received quantity
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(discrepancy.getReceivedQuantity().doubleValue());
                cell6.setCellStyle(dataStyle);
                
                // Column 7: Discrepancy quantity
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(discrepancy.getDiscrepancyQuantity().doubleValue());
                cell7.setCellStyle(typeStyle);
                
                // Column 8: Unit price
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(discrepancy.getUnitPriceUah().doubleValue());
                cell8.setCellStyle(dataStyle);
                
                // Column 9: Discrepancy value
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(discrepancy.getDiscrepancyValueUah().doubleValue());
                cell9.setCellStyle(typeStyle);
                
                // Column 10: Type
                Cell cell10 = row.createCell(10);
                String typeText = isLoss ? "ВТРАТА" : "ПРИДБАННЯ";
                cell10.setCellValue(typeText);
                cell10.setCellStyle(typeStyle);
                
                // Column 11: Comment
                Cell cell11 = row.createCell(11);
                cell11.setCellValue(discrepancy.getComment() != null ? discrepancy.getComment() : "");
                cell11.setCellStyle(dataStyle);
                
                rowNum++;
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            log.info("Excel export completed: {} records", discrepancies.size());
            return out.toByteArray();
        }
    }
}

