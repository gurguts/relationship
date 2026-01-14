package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.TransferExcelData;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.example.purchaseservice.models.warehouse.Warehouse;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.ProductTransferRepository;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.balance.IWarehouseProductBalanceService;
import org.example.purchaseservice.services.impl.IProductTransferService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.utils.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.example.purchaseservice.utils.SecurityUtils;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferService implements IProductTransferService {
    
    private static final int PRICE_SCALE = 6;
    private static final int QUANTITY_SCALE = 2;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int MAX_TRANSFERS_LIMIT = 10000;
    private static final RoundingMode QUANTITY_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    private static final String DEFAULT_SORT_PROPERTY = "transferDate";
    private static final String SORT_DIRECTION_DESC = "desc";
    private static final Set<String> VALID_SORT_PROPERTIES = Set.of(
            "id", "warehouseId", "fromProductId", "toProductId", "quantity",
            "unitPriceEur", "totalCostEur", "transferDate", "userId", "createdAt", "updatedAt"
    );
    
    private static final String UNKNOWN_WAREHOUSE = "Невідомо";
    private static final String UNKNOWN_PRODUCT = "Невідомо";
    private static final String UNKNOWN_REASON = "Не вказано";
    private static final String EXCEL_SHEET_NAME = "Переміщення";
    private static final String DATE_FORMAT_PATTERN = "dd.MM.yyyy";
    
    private static final String[] EXCEL_HEADERS = {
            "№", "Дата", "Склад", "З товару", "До товару",
            "Кількість (кг)", "Ціна за кг (грн)", "Загальна вартість (грн)",
            "Користувач ID", "Причина", "Опис"
    };
    
    private static final int EXCEL_COLUMN_COUNT = EXCEL_HEADERS.length;
    
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final ProductTransferRepository productTransferRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public ProductTransfer transferProduct(@NonNull ProductTransferDTO transferDTO) {
        log.info("Creating product transfer: warehouseId={}, fromProductId={}, toProductId={}, quantity={}", 
                transferDTO.getWarehouseId(), transferDTO.getFromProductId(), transferDTO.getToProductId(), transferDTO.getQuantity());
        validateTransferDTO(transferDTO);
        
        Long warehouseId = transferDTO.getWarehouseId();
        Long fromProductId = transferDTO.getFromProductId();
        Long toProductId = transferDTO.getToProductId();
        
        if (fromProductId.equals(toProductId)) {
            throw new PurchaseException("INVALID_TRANSFER",
                    "From product ID and to product ID cannot be the same");
        }
        
        BigDecimal quantity = transferDTO.getQuantity();
        
        var sourceBalance = warehouseProductBalanceService.getBalance(warehouseId, fromProductId);
        if (sourceBalance == null) {
            throw new PurchaseException("PRODUCT_NOT_FOUND",
                    String.format("Source product %d not found on warehouse %d", fromProductId, warehouseId));
        }
        
        BigDecimal unitPrice = sourceBalance.getAveragePriceEur();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PurchaseException("INVALID_PRICE",
                    String.format("Invalid unit price for product %d: %s", fromProductId, unitPrice));
        }
        
        BigDecimal totalCost = calculateTotalCost(quantity, unitPrice);
        
        validateAndCheckBalance(warehouseId, fromProductId, quantity);
        
        warehouseProductBalanceService.removeProductWithCost(warehouseId, fromProductId, quantity, totalCost);
        warehouseProductBalanceService.addProduct(warehouseId, toProductId, quantity, totalCost);
        
        ProductTransfer transfer = createTransfer(transferDTO, unitPrice, totalCost);
        ProductTransfer savedTransfer = productTransferRepository.save(transfer);
        
        if (savedTransfer.getCreatedAt() != null && savedTransfer.getTransferDate() == null) {
            savedTransfer.setTransferDate(savedTransfer.getCreatedAt().toLocalDate());
            savedTransfer = productTransferRepository.save(savedTransfer);
        }
        
        log.info("Product transfer created: id={}", savedTransfer.getId());
        return savedTransfer;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductTransferResponseDTO> getTransfers(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        
        validateDateRange(dateFrom, dateTo);
        validatePage(page);
        validatePageSize(size);
        String validatedSortBy = validateAndGetSortProperty(sortBy);
        Sort.Direction direction = parseSortDirection(sortDirection);
        
        Specification<ProductTransfer> spec = buildTransferSpecification(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        Sort sort = Sort.by(direction, validatedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ProductTransfer> transfers = productTransferRepository.findAll(spec, pageable);
        return transfers.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductTransferResponseDTO> getAllTransfers(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) {
        
        validateDateRange(dateFrom, dateTo);
        
        Specification<ProductTransfer> spec = buildTransferSpecification(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        List<ProductTransfer> transfers = productTransferRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY));
        
        if (transfers.size() > MAX_TRANSFERS_LIMIT) {
            throw new PurchaseException("TOO_MANY_TRANSFERS",
                    String.format("Too many transfers found: %d. Maximum allowed: %d",
                            transfers.size(), MAX_TRANSFERS_LIMIT));
        }
        
        return transfers.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public ProductTransferResponseDTO updateTransfer(@NonNull Long transferId, @NonNull ProductTransferUpdateDTO updateDTO) {
        log.info("Updating product transfer: id={}", transferId);
        ProductTransfer transfer = productTransferRepository.findById(transferId)
                .orElseThrow(() -> new PurchaseException("TRANSFER_NOT_FOUND",
                        String.format("Product transfer not found: id=%d", transferId)));

        BigDecimal unitPrice = resolveUnitPrice(transfer);
        BigDecimal originalQuantity = getSafeQuantity(transfer).setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal newQuantity = updateDTO.getQuantity() != null
                ? updateDTO.getQuantity().setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE)
                : originalQuantity;

        validateQuantity(newQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Product transfer deleted (quantity=0): id={}", transferId);
            return deleteTransfer(transfer, originalQuantity, unitPrice);
        }

        BigDecimal delta = newQuantity.subtract(originalQuantity);
        
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            handleQuantityIncrease(transfer, delta, unitPrice);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            handleQuantityDecrease(transfer, delta.abs(), unitPrice);
        }

        updateTransferFields(transfer, updateDTO, newQuantity, unitPrice);
        
        ProductTransfer saved = productTransferRepository.save(transfer);
        log.info("Product transfer updated: id={}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public ProductTransferResponseDTO toDTO(@NonNull ProductTransfer transfer) {
        ProductTransferResponseDTO dto = new ProductTransferResponseDTO();
        dto.setId(transfer.getId());
        dto.setWarehouseId(transfer.getWarehouseId());
        dto.setFromProductId(transfer.getFromProductId());
        dto.setToProductId(transfer.getToProductId());
        dto.setQuantity(transfer.getQuantity());
        dto.setUnitPriceEur(transfer.getUnitPriceEur());
        dto.setTotalCostEur(transfer.getTotalCostEur());
        dto.setTransferDate(transfer.getTransferDate());
        dto.setUserId(transfer.getUserId());
        dto.setReasonId(transfer.getReason() != null ? transfer.getReason().getId() : null);
        dto.setDescription(transfer.getDescription());
        dto.setCreatedAt(transfer.getCreatedAt());
        dto.setUpdatedAt(transfer.getUpdatedAt());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferExcelData> getTransfersForExcel(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) {
        
        validateDateRange(dateFrom, dateTo);
        
        List<ProductTransferResponseDTO> transfers = getAllTransfers(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        if (transfers.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<Long> warehouseIds = transfers.stream()
                .map(ProductTransferResponseDTO::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> productIds = transfers.stream()
                .flatMap(t -> Stream.of(t.getFromProductId(), t.getToProductId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Long> reasonIds = transfers.stream()
                .map(ProductTransferResponseDTO::getReasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, String> warehouseNames = warehouseIds.isEmpty()
                ? Collections.emptyMap()
                : warehouseRepository.findAllById(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (v1, _) -> v1));
        
        Map<Long, String> productNames = productIds.isEmpty()
                ? Collections.emptyMap()
                : StreamSupport.stream(productRepository.findAllById(productIds).spliterator(), false)
                        .collect(Collectors.toMap(Product::getId, Product::getName, (v1, _) -> v1));
        
        Map<Long, String> reasonNames = reasonIds.isEmpty()
                ? Collections.emptyMap()
                : withdrawalReasonRepository.findAllById(reasonIds).stream()
                        .collect(Collectors.toMap(WithdrawalReason::getId, WithdrawalReason::getName, (v1, _) -> v1));
        
        return transfers.stream().map(transfer -> {
            String warehouseName = warehouseNames.getOrDefault(transfer.getWarehouseId(), UNKNOWN_WAREHOUSE);
            String fromProductName = productNames.getOrDefault(transfer.getFromProductId(), UNKNOWN_PRODUCT);
            String toProductName = productNames.getOrDefault(transfer.getToProductId(), UNKNOWN_PRODUCT);
            String reasonName = transfer.getReasonId() != null
                    ? reasonNames.getOrDefault(transfer.getReasonId(), UNKNOWN_REASON)
                    : UNKNOWN_REASON;
            
            return new TransferExcelData(transfer, warehouseName, fromProductName, toProductName, reasonName);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) throws IOException {
        
        List<TransferExcelData> transfersData = getTransfersForExcel(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(EXCEL_SHEET_NAME);
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, transfersData, dataStyle);
            autoSizeColumns(sheet);
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void validateTransferDTO(@NonNull ProductTransferDTO transferDTO) {
        if (transferDTO.getWarehouseId() == null) {
            throw new PurchaseException("INVALID_TRANSFER", "Warehouse ID cannot be null");
        }
        if (transferDTO.getFromProductId() == null) {
            throw new PurchaseException("INVALID_TRANSFER", "From product ID cannot be null");
        }
        if (transferDTO.getToProductId() == null) {
            throw new PurchaseException("INVALID_TRANSFER", "To product ID cannot be null");
        }
        if (transferDTO.getQuantity() == null) {
            throw new PurchaseException("INVALID_TRANSFER", "Quantity cannot be null");
        }
        validateQuantity(transferDTO.getQuantity());
        if (transferDTO.getWithdrawalReasonId() == null) {
            throw new PurchaseException("INVALID_TRANSFER", "Withdrawal reason ID cannot be null");
        }
    }

    private void validateQuantity(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TRANSFER_QUANTITY", "Quantity cannot be negative");
        }
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new PurchaseException("INVALID_DATE_RANGE",
                    String.format("Date from (%s) cannot be after date to (%s)", dateFrom, dateTo));
        }
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new PurchaseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", page));
        }
    }

    private void validatePageSize(int size) {
        ValidationUtils.validatePageSize(size, MAX_PAGE_SIZE);
    }

    private String validateAndGetSortProperty(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return DEFAULT_SORT_PROPERTY;
        }
        if (!VALID_SORT_PROPERTIES.contains(sortBy)) {
            throw new PurchaseException("INVALID_SORT_PROPERTY",
                    String.format("Invalid sort property: %s. Valid properties: %s",
                            sortBy, String.join(", ", VALID_SORT_PROPERTIES)));
        }
        return sortBy;
    }

    private Sort.Direction parseSortDirection(String sortDirection) {
        return SORT_DIRECTION_DESC.equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }

    private Specification<ProductTransfer> buildTransferSpecification(
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

    private void validateAndCheckBalance(Long warehouseId, Long productId, BigDecimal quantity) {
        if (warehouseId == null) {
            throw new PurchaseException("INVALID_WAREHOUSE_ID", "Warehouse ID cannot be null");
        }
        if (productId == null) {
            throw new PurchaseException("INVALID_PRODUCT_ID", "Product ID cannot be null");
        }
        if (quantity == null) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be null");
        }
        
        if (!warehouseProductBalanceService.hasEnoughProduct(warehouseId, productId, quantity)) {
            var balance = warehouseProductBalanceService.getBalance(warehouseId, productId);
            throw new PurchaseException("INSUFFICIENT_PRODUCT",
                    String.format("Insufficient product on warehouse. Available: %s, requested: %s",
                            balance != null ? balance.getQuantity() : BigDecimal.ZERO, quantity));
        }
    }

    private ProductTransfer createTransfer(
            @NonNull ProductTransferDTO transferDTO,
            @NonNull BigDecimal unitPrice,
            @NonNull BigDecimal totalCost) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new PurchaseException("USER_NOT_FOUND", "Current user ID is null");
        }
        
        WithdrawalReason reason = withdrawalReasonRepository.findById(transferDTO.getWithdrawalReasonId())
                .orElseThrow(() -> new PurchaseException("REASON_NOT_FOUND",
                        "Reason not found: " + transferDTO.getWithdrawalReasonId()));
        
        ProductTransfer transfer = new ProductTransfer();
        transfer.setWarehouseId(transferDTO.getWarehouseId());
        transfer.setFromProductId(transferDTO.getFromProductId());
        transfer.setToProductId(transferDTO.getToProductId());
        transfer.setQuantity(transferDTO.getQuantity());
        transfer.setUnitPriceEur(unitPrice);
        transfer.setTotalCostEur(totalCost);
        transfer.setTransferDate(LocalDate.now());
        transfer.setUserId(userId);
        transfer.setReason(reason);
        transfer.setDescription(transferDTO.getDescription());
        
        return transfer;
    }

    private BigDecimal resolveUnitPrice(@NonNull ProductTransfer transfer) {
        BigDecimal unitPrice = transfer.getUnitPriceEur();
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice.setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
        }

        BigDecimal quantity = transfer.getQuantity();
        BigDecimal totalCost = transfer.getTotalCostEur();

        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0
                && totalCost != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(quantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
        }

        throw new PurchaseException("TRANSFER_PRICE_MISSING",
                String.format("Transfer %d is missing price information", transfer.getId()));
    }

    private BigDecimal getSafeQuantity(@NonNull ProductTransfer transfer) {
        BigDecimal quantity = transfer.getQuantity();
        if (quantity == null) {
            throw new PurchaseException("INVALID_TRANSFER", "Transfer quantity cannot be null");
        }
        return quantity;
    }

    private ProductTransferResponseDTO deleteTransfer(
            @NonNull ProductTransfer transfer,
            @NonNull BigDecimal originalQuantity,
            @NonNull BigDecimal unitPrice) {
        
        BigDecimal totalCost = transfer.getTotalCostEur() != null
                ? transfer.getTotalCostEur().setScale(PRICE_SCALE, PRICE_ROUNDING_MODE)
                : calculateTotalCost(originalQuantity, unitPrice);

        validateAndCheckBalance(transfer.getWarehouseId(), transfer.getToProductId(), originalQuantity);

        warehouseProductBalanceService.removeProductWithCost(
                transfer.getWarehouseId(),
                transfer.getToProductId(),
                originalQuantity,
                totalCost
        );

        warehouseProductBalanceService.addProduct(
                transfer.getWarehouseId(),
                transfer.getFromProductId(),
                originalQuantity,
                totalCost
        );

        productTransferRepository.delete(transfer);
        log.info("Product transfer deleted: id={}", transfer.getId());
        return null;
    }

    private void handleQuantityIncrease(
            @NonNull ProductTransfer transfer,
            @NonNull BigDecimal additionalQuantity,
            @NonNull BigDecimal unitPrice) {
        
        BigDecimal additionalCost = calculateTotalCost(additionalQuantity, unitPrice);
        validateAndCheckBalance(transfer.getWarehouseId(), transfer.getFromProductId(), additionalQuantity);
        transferProductBetweenBalances(
                transfer.getWarehouseId(),
                transfer.getFromProductId(),
                transfer.getToProductId(),
                additionalQuantity,
                additionalCost
        );
    }

    private void handleQuantityDecrease(
            @NonNull ProductTransfer transfer,
            @NonNull BigDecimal quantityToReturn,
            @NonNull BigDecimal unitPrice) {
        
        BigDecimal costToReturn = calculateTotalCost(quantityToReturn, unitPrice);
        validateAndCheckBalance(transfer.getWarehouseId(), transfer.getToProductId(), quantityToReturn);
        transferProductBetweenBalances(
                transfer.getWarehouseId(),
                transfer.getToProductId(),
                transfer.getFromProductId(),
                quantityToReturn,
                costToReturn
        );
    }

    private void transferProductBetweenBalances(
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            BigDecimal quantity,
            BigDecimal totalCost) {
        
        warehouseProductBalanceService.removeProductWithCost(warehouseId, fromProductId, quantity, totalCost);
        warehouseProductBalanceService.addProduct(warehouseId, toProductId, quantity, totalCost);
    }

    private BigDecimal calculateTotalCost(@NonNull BigDecimal quantity, @NonNull BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(PRICE_SCALE, PRICE_ROUNDING_MODE);
    }

    private void updateTransferFields(
            @NonNull ProductTransfer transfer,
            @NonNull ProductTransferUpdateDTO updateDTO,
            @NonNull BigDecimal newQuantity,
            @NonNull BigDecimal unitPrice) {
        
        if (updateDTO.getReasonId() != null &&
                (transfer.getReason() == null || !updateDTO.getReasonId().equals(transfer.getReason().getId()))) {
            WithdrawalReason reason = withdrawalReasonRepository.findById(updateDTO.getReasonId())
                    .orElseThrow(() -> new PurchaseException("REASON_NOT_FOUND",
                            "Reason not found: " + updateDTO.getReasonId()));

            if (reason.getPurpose() != WithdrawalReason.Purpose.BOTH) {
                throw new PurchaseException("INVALID_REASON_PURPOSE",
                        "For transfers, only reasons with BOTH purpose type can be selected");
            }

            transfer.setReason(reason);
        }

        if (updateDTO.getDescription() != null) {
            transfer.setDescription(updateDTO.getDescription());
        }

        transfer.setQuantity(newQuantity);
        transfer.setUnitPriceEur(unitPrice);
        transfer.setTotalCostEur(calculateTotalCost(newQuantity, unitPrice));
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
            @NonNull List<TransferExcelData> transfersData,
            @NonNull CellStyle dataStyle) {
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
        int rowIndex = 1;
        
        for (TransferExcelData data : transfersData) {
            Row row = sheet.createRow(rowIndex);
            fillTransferRow(row, data, rowIndex, dateFormatter, dataStyle);
            rowIndex++;
        }
    }

    private void fillTransferRow(
            @NonNull Row row,
            @NonNull TransferExcelData data,
            int rowNumber,
            @NonNull DateTimeFormatter dateFormatter,
            @NonNull CellStyle dataStyle) {
        
        ProductTransferResponseDTO transfer = data.transfer();
        
        createCell(row, 0, rowNumber, dataStyle);
        createCell(row, 1, formatTransferDate(transfer.getTransferDate(), dateFormatter), dataStyle);
        createCell(row, 2, data.warehouseName(), dataStyle);
        createCell(row, 3, data.fromProductName(), dataStyle);
        createCell(row, 4, data.toProductName(), dataStyle);
        createCell(row, 5, getBigDecimalValue(transfer.getQuantity()), dataStyle);
        createCell(row, 6, getBigDecimalValue(transfer.getUnitPriceEur()), dataStyle);
        createCell(row, 7, getBigDecimalValue(transfer.getTotalCostEur()), dataStyle);
        createCell(row, 8, transfer.getUserId() != null ? transfer.getUserId() : "", dataStyle);
        createCell(row, 9, data.reasonName(), dataStyle);
        createCell(row, 10, transfer.getDescription() != null ? transfer.getDescription() : "", dataStyle);
    }

    private String formatTransferDate(LocalDate date, @NonNull DateTimeFormatter formatter) {
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
        for (int i = 0; i < ProductTransferService.EXCEL_COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
