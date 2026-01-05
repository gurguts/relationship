package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.ProductTransferRepository;
import org.example.purchaseservice.repositories.WarehouseRepository;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.balance.IWarehouseProductBalanceService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferService {
    
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final ProductTransferRepository productTransferRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    
    /**
     * Transfer product from one product to another within the same warehouse
     * Uses average price from source product balance
     * Removes quantity*averagePrice from source product and adds it to target product
     * Creates a ProductTransfer record for audit trail
     * 
     * @param transferDTO transfer details
     * @return created ProductTransfer record
     */
    @Transactional
    public ProductTransfer transferProduct(ProductTransferDTO transferDTO) {
        log.info("Transferring product: from {} to {}, quantity={}", 
                transferDTO.getFromProductId(), 
                transferDTO.getToProductId(),
                transferDTO.getQuantity());
        
        // Get source product balance to determine price
        var sourceBalance = warehouseProductBalanceService.getBalance(
                transferDTO.getWarehouseId(),
                transferDTO.getFromProductId());
        
        if (sourceBalance == null) {
            throw new PurchaseException("PRODUCT_NOT_FOUND", String.format(
                    "Source product %d not found on warehouse %d",
                    transferDTO.getFromProductId(),
                    transferDTO.getWarehouseId()));
        }
        
        // Get average price from source product
        BigDecimal unitPrice = sourceBalance.getAveragePriceEur();
        
        // Calculate total cost based on source product's average price
        BigDecimal totalCost = transferDTO.getQuantity().multiply(unitPrice);
        
        log.info("Using price from source product: {} UAH/kg, total cost: {} UAH", 
                unitPrice, totalCost);
        
        // Validate source product has enough quantity
        if (!warehouseProductBalanceService.hasEnoughProduct(
                transferDTO.getWarehouseId(),
                transferDTO.getFromProductId(),
                transferDTO.getQuantity())) {
            
            var balance = warehouseProductBalanceService.getBalance(
                    transferDTO.getWarehouseId(),
                    transferDTO.getFromProductId());
            
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: %s, requested: %s",
                    balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                    transferDTO.getQuantity()));
        }
        
        // Remove from source product with specific cost
        warehouseProductBalanceService.removeProductWithCost(
                transferDTO.getWarehouseId(),
                transferDTO.getFromProductId(),
                transferDTO.getQuantity(),
                totalCost
        );
        
        log.info("Removed {} kg (cost: {} UAH) from product {}", 
                transferDTO.getQuantity(), totalCost, transferDTO.getFromProductId());
        
        // Add to target product with same cost
        warehouseProductBalanceService.addProduct(
                transferDTO.getWarehouseId(),
                transferDTO.getToProductId(),
                transferDTO.getQuantity(),
                totalCost
        );
        
        log.info("Added {} kg (cost: {} UAH) to product {}", 
                transferDTO.getQuantity(), totalCost, transferDTO.getToProductId());
        
        // Create transfer record for audit trail
        Long userId = SecurityUtils.getCurrentUserId();
        
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
        // Set transferDate to current date if not already set (will be set from createdAt after save)
        if (transfer.getTransferDate() == null) {
            transfer.setTransferDate(LocalDate.now());
        }
        transfer.setUserId(userId);
        transfer.setReason(reason);
        transfer.setDescription(transferDTO.getDescription());
        
        ProductTransfer savedTransfer = productTransferRepository.save(transfer);
        
        // Update transferDate from createdAt after save to ensure consistency
        if (savedTransfer.getCreatedAt() != null) {
            savedTransfer.setTransferDate(savedTransfer.getCreatedAt().toLocalDate());
            savedTransfer = productTransferRepository.save(savedTransfer);
        }
        
        log.info("Product transfer completed. Transfer record created: id={}", savedTransfer.getId());
        
        return savedTransfer;
    }
    
    /**
     * Get all transfers with filtering, sorting and pagination
     */
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
        
        Specification<ProductTransfer> spec = (root, query, cb) -> {
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
        
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "transferDate"
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ProductTransfer> transfers = productTransferRepository.findAll(spec, pageable);
        
        return transfers.map(this::toDTO);
    }
    
    /**
     * Get all transfers (for Excel export)
     */
    @Transactional(readOnly = true)
    public List<ProductTransferResponseDTO> getAllTransfers(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) {
        
        Specification<ProductTransfer> spec = (root, query, cb) -> {
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
        
        List<ProductTransfer> transfers = productTransferRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "transferDate"));
        
        return transfers.stream().map(this::toDTO).toList();
    }

    /**
     * Update existing transfer (quantity, reason, description)
     */
    @Transactional
    public ProductTransferResponseDTO updateTransfer(Long transferId, ProductTransferUpdateDTO updateDTO) {
        ProductTransfer transfer = productTransferRepository.findById(transferId)
                .orElseThrow(() -> new PurchaseException("TRANSFER_NOT_FOUND",
                        String.format("Product transfer not found: id=%d", transferId)));

        BigDecimal unitPrice = resolveUnitPrice(transfer);
        BigDecimal originalQuantity = transfer.getQuantity().setScale(2, RoundingMode.HALF_UP);
        BigDecimal newQuantity = updateDTO.getQuantity() != null
                ? updateDTO.getQuantity().setScale(2, RoundingMode.HALF_UP)
                : originalQuantity;

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TRANSFER_QUANTITY", "Quantity cannot be negative");
        }

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal totalCost = transfer.getTotalCostEur() != null
                    ? transfer.getTotalCostEur().setScale(6, RoundingMode.HALF_UP)
                    : originalQuantity.multiply(unitPrice).setScale(6, RoundingMode.HALF_UP);

            if (!warehouseProductBalanceService.hasEnoughProduct(transfer.getWarehouseId(),
                    transfer.getToProductId(), originalQuantity)) {
                var balance = warehouseProductBalanceService.getBalance(
                        transfer.getWarehouseId(), transfer.getToProductId());
                throw new PurchaseException("INSUFFICIENT_PRODUCT_TARGET", String.format(
                        "На товарі-приймачі недостатньо кількості. Доступно: %s, потрібно повернути: %s",
                        balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                        originalQuantity));
            }

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
            return null;
        }

        BigDecimal delta = newQuantity.subtract(originalQuantity);

        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal additionalQuantity = delta.setScale(2, RoundingMode.HALF_UP);
            BigDecimal additionalCost = additionalQuantity.multiply(unitPrice).setScale(6, RoundingMode.HALF_UP);

            if (!warehouseProductBalanceService.hasEnoughProduct(transfer.getWarehouseId(),
                    transfer.getFromProductId(), additionalQuantity)) {
                var balance = warehouseProductBalanceService.getBalance(
                        transfer.getWarehouseId(), transfer.getFromProductId());
                throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                        "Недостатньо товару на складі. Доступно: %s, потрібно додатково: %s",
                        balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                        additionalQuantity));
            }

            warehouseProductBalanceService.removeProductWithCost(
                    transfer.getWarehouseId(),
                    transfer.getFromProductId(),
                    additionalQuantity,
                    additionalCost
            );

            warehouseProductBalanceService.addProduct(
                    transfer.getWarehouseId(),
                    transfer.getToProductId(),
                    additionalQuantity,
                    additionalCost
            );
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal quantityToReturn = delta.abs().setScale(2, RoundingMode.HALF_UP);
            BigDecimal costToReturn = quantityToReturn.multiply(unitPrice).setScale(6, RoundingMode.HALF_UP);

            if (!warehouseProductBalanceService.hasEnoughProduct(transfer.getWarehouseId(),
                    transfer.getToProductId(), quantityToReturn)) {
                var balance = warehouseProductBalanceService.getBalance(
                        transfer.getWarehouseId(), transfer.getToProductId());
                throw new PurchaseException("INSUFFICIENT_PRODUCT_TARGET", String.format(
                        "На товарі-приймачі недостатньо кількості. Доступно: %s, потрібно повернути: %s",
                        balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                        quantityToReturn));
            }

            warehouseProductBalanceService.removeProductWithCost(
                    transfer.getWarehouseId(),
                    transfer.getToProductId(),
                    quantityToReturn,
                    costToReturn
            );

            warehouseProductBalanceService.addProduct(
                    transfer.getWarehouseId(),
                    transfer.getFromProductId(),
                    quantityToReturn,
                    costToReturn
            );
        }

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
        transfer.setTotalCostEur(newQuantity.multiply(unitPrice).setScale(6, RoundingMode.HALF_UP));

        ProductTransfer saved = productTransferRepository.save(transfer);
        return toDTO(saved);
    }

    public ProductTransferResponseDTO toDTO(ProductTransfer transfer) {
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

    private BigDecimal resolveUnitPrice(ProductTransfer transfer) {
        BigDecimal unitPrice = transfer.getUnitPriceEur();
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice.setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal quantity = transfer.getQuantity();
        BigDecimal totalCost = transfer.getTotalCostEur();

        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0
                && totalCost != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            // Round up to avoid loss of precision
            return totalCost.divide(quantity, 6, RoundingMode.CEILING);
        }

        throw new PurchaseException("TRANSFER_PRICE_MISSING",
                String.format("Transfer %d is missing price information", transfer.getId()));
    }

    public record TransferExcelData(
            ProductTransferResponseDTO transfer,
            String warehouseName,
            String fromProductName,
            String toProductName,
            String reasonName
    ) {}

    @Transactional(readOnly = true)
    public List<TransferExcelData> getTransfersForExcel(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) {
        
        List<ProductTransferResponseDTO> transfers = getAllTransfers(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        return transfers.stream().map(transfer -> {
            String warehouseName = warehouseRepository.findById(transfer.getWarehouseId())
                    .map(w -> w.getName())
                    .orElse("Невідомо");
            String fromProductName = productRepository.findById(transfer.getFromProductId())
                    .map(p -> p.getName())
                    .orElse("Невідомо");
            String toProductName = productRepository.findById(transfer.getToProductId())
                    .map(p -> p.getName())
                    .orElse("Невідомо");
            String reasonName = transfer.getReasonId() != null 
                    ? withdrawalReasonRepository.findById(transfer.getReasonId())
                            .map(r -> r.getName())
                            .orElse("Не вказано")
                    : "Не вказано";
            
            return new TransferExcelData(transfer, warehouseName, fromProductName, toProductName, reasonName);
        }).toList();
    }

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
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Переміщення");

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

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"№", "Дата", "Склад", "З товару", "До товару", 
                               "Кількість (кг)", "Ціна за кг (грн)", "Загальна вартість (грн)", 
                               "Користувач ID", "Причина", "Опис"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            int rowNum = 1;
            
            for (TransferExcelData data : transfersData) {
                Row row = sheet.createRow(rowNum);
                ProductTransferResponseDTO transfer = data.transfer();

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(transfer.getTransferDate().format(dateFormatter));
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(data.warehouseName());
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(data.fromProductName());
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(data.toProductName());
                cell4.setCellStyle(dataStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(transfer.getQuantity().doubleValue());
                cell5.setCellStyle(dataStyle);

                Cell cell6 = row.createCell(6);
                cell6.setCellValue(transfer.getUnitPriceEur().doubleValue());
                cell6.setCellStyle(dataStyle);

                Cell cell7 = row.createCell(7);
                cell7.setCellValue(transfer.getTotalCostEur().doubleValue());
                cell7.setCellStyle(dataStyle);

                Cell cell8 = row.createCell(8);
                cell8.setCellValue(transfer.getUserId());
                cell8.setCellStyle(dataStyle);

                Cell cell9 = row.createCell(9);
                cell9.setCellValue(data.reasonName());
                cell9.setCellStyle(dataStyle);

                Cell cell10 = row.createCell(10);
                cell10.setCellValue(transfer.getDescription() != null ? transfer.getDescription() : "");
                cell10.setCellStyle(dataStyle);
                
                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}

