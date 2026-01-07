package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.repositories.WarehouseDiscrepancyRepository;
import org.example.purchaseservice.models.dto.warehouse.DiscrepancyStatisticsDTO;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseDiscrepancyService {
    
    private static final int VALUE_SCALE = 2;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final RoundingMode VALUE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    private static final String UNKNOWN_PRODUCT = "Невідомо";
    private static final String UNKNOWN_WAREHOUSE = "Невідомо";
    private static final String TYPE_LOSS = "ВТРАТА";
    private static final String TYPE_GAIN = "ПРИДБАННЯ";
    private static final String EXCEL_SHEET_NAME = "Втрати та придбання";
    private static final String DATE_FORMAT_PATTERN = "dd.MM.yyyy";
    
    private static final String[] EXCEL_HEADERS = {
            "№", "Дата", "Водій ID", "Товар", "Склад",
            "Закуплено (кг)", "Прийнято (кг)", "Різниця (кг)",
            "Ціна за кг (грн)", "Вартість різниці (грн)", "Тип", "Коментар"
    };
    
    private static final int EXCEL_COLUMN_COUNT = EXCEL_HEADERS.length;
    private static final Set<String> VALID_DISCREPANCY_TYPES = Set.of("LOSS", "GAIN");
    
    private final WarehouseDiscrepancyRepository discrepancyRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    
    @Transactional
    public void createDiscrepancy(
            Long warehouseReceiptId,
            Long driverId,
            Long productId,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal purchasedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal unitPriceEur,
            Long createdByUserId,
            String comment) {
        
        validateDiscrepancyParams(driverId, productId, warehouseId, receiptDate, 
                purchasedQuantity, receivedQuantity, unitPriceEur);
        
        BigDecimal discrepancyQuantity = receivedQuantity.subtract(purchasedQuantity);
        
        if (discrepancyQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        
        BigDecimal discrepancyValue = calculateDiscrepancyValue(discrepancyQuantity, unitPriceEur);
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
                .unitPriceEur(unitPriceEur)
                .discrepancyValueEur(discrepancyValue)
                .type(type)
                .comment(comment)
                .createdByUserId(createdByUserId)
                .build();

        discrepancyRepository.save(discrepancy);
    }
    
    @Transactional
    public void createFromDriverBalance(
            @NonNull Long warehouseReceiptId,
            @NonNull DriverProductBalance driverBalance,
            @NonNull Long warehouseId,
            @NonNull LocalDate receiptDate,
            @NonNull BigDecimal receivedQuantity,
            Long createdByUserId,
            String comment) {
        
        if (driverBalance.getDriverId() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Driver ID cannot be null");
        }
        if (driverBalance.getProductId() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Product ID cannot be null");
        }
        if (driverBalance.getQuantity() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Quantity cannot be null");
        }
        if (driverBalance.getAveragePriceEur() == null) {
            throw new PurchaseException("INVALID_DRIVER_BALANCE", "Average price cannot be null");
        }

        createDiscrepancy(
                warehouseReceiptId,
                driverBalance.getDriverId(),
                driverBalance.getProductId(),
                warehouseId,
                receiptDate,
                driverBalance.getQuantity(),
                receivedQuantity,
                driverBalance.getAveragePriceEur(),
                createdByUserId,
                comment
        );
    }

    @Transactional(readOnly = true)
    public Page<WarehouseDiscrepancy> getDiscrepancies(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable) {
        
        validateDateRange(dateFrom, dateTo);
        validatePageable(pageable);
        validateDiscrepancyType(type);
        
        Specification<WarehouseDiscrepancy> spec = buildSpecification(
                driverId, productId, warehouseId, type, dateFrom, dateTo);
        return discrepancyRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public DiscrepancyStatisticsDTO getStatistics(String type, LocalDate dateFrom, LocalDate dateTo) {
        validateDateRange(dateFrom, dateTo);
        validateDiscrepancyType(type);
        
        Specification<WarehouseDiscrepancy> spec = buildSpecification(null, null, null, type, dateFrom, dateTo);
        List<WarehouseDiscrepancy> filteredDiscrepancies = discrepancyRepository.findAll(spec);

        StatisticsAccumulator accumulator = filteredDiscrepancies.stream()
                .collect(StatisticsAccumulator::new,
                        StatisticsAccumulator::accumulate,
                        StatisticsAccumulator::combine);

        DiscrepancyStatisticsDTO stats = new DiscrepancyStatisticsDTO();
        stats.setTotalLossesValue(accumulator.getTotalLosses());
        stats.setTotalGainsValue(accumulator.getTotalGains());
        stats.setLossCount(accumulator.getLossCount());
        stats.setGainCount(accumulator.getGainCount());
        stats.setNetValue(accumulator.getTotalGains().add(accumulator.getTotalLosses().negate()));
        
        return stats;
    }

    @Transactional(readOnly = true)
    public WarehouseDiscrepancy getDiscrepancyById(@NonNull Long id) {
        return discrepancyRepository.findById(id)
                .orElseThrow(() -> new PurchaseException("DISCREPANCY_NOT_FOUND",
                        String.format("Discrepancy not found: id=%d", id)));
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel(
            Long driverId,
            Long productId,
            Long warehouseId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo) throws IOException {
        
        validateDateRange(dateFrom, dateTo);
        validateDiscrepancyType(type);
        
        Specification<WarehouseDiscrepancy> spec = buildSpecification(
                driverId, productId, warehouseId, type, dateFrom, dateTo);
        List<WarehouseDiscrepancy> discrepancies = discrepancyRepository.findAll(spec);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(EXCEL_SHEET_NAME);
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle lossStyle = createLossStyle(workbook, dataStyle);
            CellStyle gainStyle = createGainStyle(workbook, dataStyle);
            
            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, discrepancies, dataStyle, lossStyle, gainStyle);
            autoSizeColumns(sheet);
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void validateDiscrepancyParams(
            Long driverId,
            Long productId,
            Long warehouseId,
            LocalDate receiptDate,
            BigDecimal purchasedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal unitPriceEur) {
        
        if (driverId == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Driver ID cannot be null");
        }
        if (productId == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Product ID cannot be null");
        }
        if (warehouseId == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Warehouse ID cannot be null");
        }
        if (receiptDate == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Receipt date cannot be null");
        }
        if (purchasedQuantity == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Purchased quantity cannot be null");
        }
        if (receivedQuantity == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Received quantity cannot be null");
        }
        if (unitPriceEur == null) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Unit price cannot be null");
        }
        
        if (purchasedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Purchased quantity cannot be negative");
        }
        if (receivedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Received quantity cannot be negative");
        }
        if (unitPriceEur.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_DISCREPANCY", "Unit price must be positive");
        }
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new PurchaseException("INVALID_DATE_RANGE",
                    String.format("Date from (%s) cannot be after date to (%s)", dateFrom, dateTo));
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new PurchaseException("INVALID_PAGEABLE", "Pageable cannot be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new PurchaseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", pageable.getPageNumber()));
        }
        if (pageable.getPageSize() <= 0) {
            throw new PurchaseException("INVALID_PAGE_SIZE", "Page size must be positive");
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new PurchaseException("INVALID_PAGE_SIZE",
                    String.format("Page size cannot exceed %d, got: %d", MAX_PAGE_SIZE, pageable.getPageSize()));
        }
    }

    private void validateDiscrepancyType(String type) {
        if (type != null && !type.trim().isEmpty() && !VALID_DISCREPANCY_TYPES.contains(type.toUpperCase())) {
            throw new PurchaseException("INVALID_DISCREPANCY_TYPE",
                    String.format("Invalid discrepancy type: %s. Valid types: %s",
                            type, String.join(", ", VALID_DISCREPANCY_TYPES)));
        }
    }

    private BigDecimal calculateDiscrepancyValue(@NonNull BigDecimal discrepancyQuantity, @NonNull BigDecimal unitPriceEur) {
        return discrepancyQuantity.multiply(unitPriceEur)
                .setScale(VALUE_SCALE, VALUE_ROUNDING_MODE);
    }

    private Specification<WarehouseDiscrepancy> buildSpecification(
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

    private CellStyle createHeaderStyle(@NonNull Workbook workbook) {
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
        return headerStyle;
    }

    private CellStyle createDataStyle(@NonNull Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        return dataStyle;
    }

    private CellStyle createLossStyle(@NonNull Workbook workbook, @NonNull CellStyle baseStyle) {
        CellStyle lossStyle = workbook.createCellStyle();
        lossStyle.cloneStyleFrom(baseStyle);
        Font lossFont = workbook.createFont();
        lossFont.setColor(IndexedColors.RED.getIndex());
        lossStyle.setFont(lossFont);
        return lossStyle;
    }

    private CellStyle createGainStyle(@NonNull Workbook workbook, @NonNull CellStyle baseStyle) {
        CellStyle gainStyle = workbook.createCellStyle();
        gainStyle.cloneStyleFrom(baseStyle);
        Font gainFont = workbook.createFont();
        gainFont.setColor(IndexedColors.GREEN.getIndex());
        gainStyle.setFont(gainFont);
        return gainStyle;
    }

    private void createHeaderRow(@NonNull Sheet sheet, @NonNull CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < EXCEL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(EXCEL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillDataRows(
            @NonNull Sheet sheet,
            @NonNull List<WarehouseDiscrepancy> discrepancies,
            @NonNull CellStyle dataStyle,
            @NonNull CellStyle lossStyle,
            @NonNull CellStyle gainStyle) {
        
        if (discrepancies.isEmpty()) {
            return;
        }
        
        Map<Long, String> productNames = loadProductNames(discrepancies);
        Map<Long, String> warehouseNames = loadWarehouseNames(discrepancies);
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
        int rowIndex = 1;
        
        for (WarehouseDiscrepancy discrepancy : discrepancies) {
            Row row = sheet.createRow(rowIndex);
            fillDiscrepancyRow(row, discrepancy, rowIndex, productNames, warehouseNames, 
                    dateFormatter, dataStyle, lossStyle, gainStyle);
            rowIndex++;
        }
    }

    private Map<Long, String> loadProductNames(@NonNull List<WarehouseDiscrepancy> discrepancies) {
        Set<Long> productIds = discrepancies.stream()
                .map(WarehouseDiscrepancy::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return StreamSupport.stream(productRepository.findAllById(productIds).spliterator(), false)
                .collect(Collectors.toMap(Product::getId, Product::getName, (v1, _) -> v1));
    }

    private Map<Long, String> loadWarehouseNames(@NonNull List<WarehouseDiscrepancy> discrepancies) {
        Set<Long> warehouseIds = discrepancies.stream()
                .map(WarehouseDiscrepancy::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (v1, _) -> v1));
    }

    private void fillDiscrepancyRow(
            @NonNull Row row,
            @NonNull WarehouseDiscrepancy discrepancy,
            int rowNumber,
            @NonNull Map<Long, String> productNames,
            @NonNull Map<Long, String> warehouseNames,
            @NonNull DateTimeFormatter dateFormatter,
            @NonNull CellStyle dataStyle,
            @NonNull CellStyle lossStyle,
            @NonNull CellStyle gainStyle) {
        
        boolean isLoss = discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS;
        CellStyle typeStyle = isLoss ? lossStyle : gainStyle;
        
        String productName = productNames.getOrDefault(discrepancy.getProductId(), UNKNOWN_PRODUCT);
        String warehouseName = warehouseNames.getOrDefault(discrepancy.getWarehouseId(), UNKNOWN_WAREHOUSE);
        String typeText = isLoss ? TYPE_LOSS : TYPE_GAIN;
        
        createCell(row, 0, rowNumber, dataStyle);
        createCell(row, 1, formatDate(discrepancy.getReceiptDate(), dateFormatter), dataStyle);
        createCell(row, 2, discrepancy.getDriverId(), dataStyle);
        createCell(row, 3, productName, dataStyle);
        createCell(row, 4, warehouseName, dataStyle);
        createCell(row, 5, getBigDecimalValue(discrepancy.getPurchasedQuantity()), dataStyle);
        createCell(row, 6, getBigDecimalValue(discrepancy.getReceivedQuantity()), dataStyle);
        createCell(row, 7, getBigDecimalValue(discrepancy.getDiscrepancyQuantity()), typeStyle);
        createCell(row, 8, getBigDecimalValue(discrepancy.getUnitPriceEur()), dataStyle);
        createCell(row, 9, getBigDecimalValue(discrepancy.getDiscrepancyValueEur()), typeStyle);
        createCell(row, 10, typeText, typeStyle);
        createCell(row, 11, discrepancy.getComment() != null ? discrepancy.getComment() : "", dataStyle);
    }

    private String formatDate(LocalDate date, @NonNull DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : "";
    }

    private double getBigDecimalValue(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private void createCell(@NonNull Row row, int columnIndex, Object value, @NonNull CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        
        switch (value) {
            case BigDecimal bigDecimal -> cell.setCellValue(bigDecimal.doubleValue());
            case Number number -> {
                if (value instanceof Long || value instanceof Integer) {
                    cell.setCellValue(number.longValue());
                } else {
                    cell.setCellValue(number.doubleValue());
                }
            }
            case String s -> cell.setCellValue(s);
            default -> cell.setCellValue(value.toString());
        }
    }

    private void autoSizeColumns(@NonNull Sheet sheet) {
        for (int i = 0; i < EXCEL_COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static class StatisticsAccumulator {
        private BigDecimal totalLosses = BigDecimal.ZERO;
        private BigDecimal totalGains = BigDecimal.ZERO;
        private long lossCount = 0;
        private long gainCount = 0;

        void accumulate(WarehouseDiscrepancy discrepancy) {
            BigDecimal value = discrepancy.getDiscrepancyValueEur();
            if (value == null) {
                return;
            }
            
            if (discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS) {
                totalLosses = totalLosses.add(value.abs());
                lossCount++;
            } else if (discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.GAIN) {
                totalGains = totalGains.add(value);
                gainCount++;
            }
        }

        void combine(StatisticsAccumulator other) {
            this.totalLosses = this.totalLosses.add(other.totalLosses);
            this.totalGains = this.totalGains.add(other.totalGains);
            this.lossCount += other.lossCount;
            this.gainCount += other.gainCount;
        }

        BigDecimal getTotalLosses() {
            return totalLosses;
        }

        BigDecimal getTotalGains() {
            return totalGains;
        }

        long getLossCount() {
            return lossCount;
        }

        long getGainCount() {
            return gainCount;
        }
    }
}
