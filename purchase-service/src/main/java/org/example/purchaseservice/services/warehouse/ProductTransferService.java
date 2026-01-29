package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.TransferExcelData;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.example.purchaseservice.repositories.ProductTransferRepository;
import org.example.purchaseservice.services.impl.IProductTransferService;
import org.example.purchaseservice.services.impl.IWarehouseProductBalanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferService implements IProductTransferService {
    
    private static final int QUANTITY_SCALE = 2;
    private static final int MAX_TRANSFERS_LIMIT = 10000;
    private static final java.math.RoundingMode QUANTITY_ROUNDING_MODE = java.math.RoundingMode.HALF_UP;
    
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final ProductTransferRepository productTransferRepository;
    private final ProductTransferValidator validator;
    private final ProductTransferSpecificationBuilder specificationBuilder;
    private final ProductTransferCalculator calculator;
    private final ProductTransferBalanceHandler balanceHandler;
    private final ProductTransferFactory factory;
    private final ProductTransferExcelDataFetcher excelDataFetcher;
    private final ProductTransferExcelGenerator excelGenerator;

    @Override
    @Transactional
    public ProductTransfer transferProduct(@NonNull ProductTransferDTO transferDTO) {
        log.info("Creating product transfer: warehouseId={}, fromProductId={}, toProductId={}, quantity={}", 
                transferDTO.getWarehouseId(), transferDTO.getFromProductId(), transferDTO.getToProductId(), transferDTO.getQuantity());
        validator.validateTransferDTO(transferDTO);
        
        Long warehouseId = transferDTO.getWarehouseId();
        Long fromProductId = transferDTO.getFromProductId();
        Long toProductId = transferDTO.getToProductId();
        
        validator.validateSameProductIds(fromProductId, toProductId);
        
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
        
        BigDecimal totalCost = calculator.calculateTotalCost(quantity, unitPrice);
        
        balanceHandler.validateAndCheckBalance(warehouseId, fromProductId, quantity);
        
        balanceHandler.transferProductBetweenBalances(warehouseId, fromProductId, toProductId, quantity, totalCost);
        
        ProductTransfer transfer = factory.createTransfer(transferDTO, unitPrice, totalCost);
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
        
        validator.validateDateRange(dateFrom, dateTo);
        validator.validatePage(page);
        validator.validatePageSize(size);
        String validatedSortBy = specificationBuilder.validateAndGetSortProperty(sortBy);
        Sort.Direction direction = specificationBuilder.parseSortDirection(sortDirection);
        
        var spec = specificationBuilder.buildTransferSpecification(
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
        
        validator.validateDateRange(dateFrom, dateTo);
        
        var spec = specificationBuilder.buildTransferSpecification(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        List<ProductTransfer> transfers = productTransferRepository.findAll(
                spec, specificationBuilder.createDefaultSort());
        
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

        BigDecimal unitPrice = calculator.resolveUnitPrice(transfer);
        BigDecimal originalQuantity = factory.getSafeQuantity(transfer).setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE);
        BigDecimal newQuantity = updateDTO.getQuantity() != null
                ? updateDTO.getQuantity().setScale(QUANTITY_SCALE, QUANTITY_ROUNDING_MODE)
                : originalQuantity;

        validator.validateQuantity(newQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Product transfer deleted (quantity=0): id={}", transferId);
            return deleteTransfer(transfer, originalQuantity, unitPrice);
        }

        BigDecimal delta = newQuantity.subtract(originalQuantity);
        
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            balanceHandler.handleQuantityIncrease(transfer, delta, unitPrice, calculator);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            balanceHandler.handleQuantityDecrease(transfer, delta.abs(), unitPrice, calculator);
        }

        factory.updateTransferFields(transfer, updateDTO, newQuantity, unitPrice);
        
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
        
        validator.validateDateRange(dateFrom, dateTo);
        
        List<ProductTransferResponseDTO> transfers = getAllTransfers(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
        
        return excelDataFetcher.prepareExcelData(transfers);
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
        
        return excelGenerator.generateExcel(transfersData);
    }

    private ProductTransferResponseDTO deleteTransfer(
            @NonNull ProductTransfer transfer,
            @NonNull BigDecimal originalQuantity,
            @NonNull BigDecimal unitPrice) {
        
        BigDecimal totalCost = transfer.getTotalCostEur() != null
                ? transfer.getTotalCostEur().setScale(ProductTransferCalculator.PRICE_SCALE, ProductTransferCalculator.PRICE_ROUNDING_MODE)
                : calculator.calculateTotalCost(originalQuantity, unitPrice);

        balanceHandler.validateAndCheckBalance(transfer.getWarehouseId(), transfer.getToProductId(), originalQuantity);

        balanceHandler.transferProductBetweenBalances(
                transfer.getWarehouseId(),
                transfer.getToProductId(),
                transfer.getFromProductId(),
                originalQuantity,
                totalCost
        );

        productTransferRepository.delete(transfer);
        log.info("Product transfer deleted: id={}", transfer.getId());
        return null;
    }
}
