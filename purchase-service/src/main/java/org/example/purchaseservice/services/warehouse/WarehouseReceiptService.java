package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.mappers.WarehouseReceiptMapper;
import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptDTO;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.services.impl.IDriverProductBalanceService;
import org.example.purchaseservice.services.impl.IWarehouseDiscrepancyService;
import org.example.purchaseservice.services.impl.IWarehouseReceiptService;
import org.example.purchaseservice.spec.WarehouseReceiptFilterBuilder;
import org.example.purchaseservice.spec.WarehouseReceiptSpecification;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseReceiptService implements IWarehouseReceiptService {
    
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final IDriverProductBalanceService driverProductBalanceService;
    private final IWarehouseDiscrepancyService warehouseDiscrepancyService;
    private final WarehouseReceiptValidator validator;
    private final WarehouseReceiptCalculator calculator;
    private final WarehouseReceiptBalanceHandler balanceHandler;
    private final WarehouseReceiptMapper mapper;
    private final WarehouseReceiptFilterBuilder filterBuilder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseReceiptDTO> getWarehouseReceipts(
            int page, 
            int size, 
            String sort,
            String direction, 
            Map<String, List<String>> filters) {
        
        validator.validatePage(page);
        validator.validatePageSize(size);
        validator.validateSortParams(sort, direction);
        validator.validateFilters(filters);
        
        PageRequest pageRequest = createPageRequest(page, size, sort, direction);
        WarehouseReceiptSpecification spec = new WarehouseReceiptSpecification(filters, filterBuilder);
        Page<WarehouseReceipt> warehouseReceiptPage = warehouseReceiptRepository.findAll(spec, pageRequest);

        List<WarehouseReceiptDTO> content = warehouseReceiptPage.getContent().stream()
                .map(mapper::warehouseReceiptToWarehouseReceiptDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                warehouseReceiptPage.getNumber(),
                warehouseReceiptPage.getSize(),
                (int) warehouseReceiptPage.getTotalElements(),
                warehouseReceiptPage.getTotalPages(),
                content
        );
    }
    
    @Override
    @Transactional
    public WarehouseReceipt createWarehouseReceipt(@NonNull WarehouseReceipt warehouseReceipt) {
        log.info("Creating warehouse receipt: userId={}, productId={}, warehouseId={}, quantity={}", 
                warehouseReceipt.getUserId(), warehouseReceipt.getProductId(), warehouseReceipt.getWarehouseId(), warehouseReceipt.getQuantity());
        validator.validateWarehouseReceipt(warehouseReceipt);
        
        DriverProductBalance driverBalance = driverProductBalanceService.getBalance(
                warehouseReceipt.getUserId(), 
                warehouseReceipt.getProductId());
        
        validator.validateDriverBalance(driverBalance, warehouseReceipt.getUserId(), warehouseReceipt.getProductId());
        
        Long executorUserId = SecurityUtils.getCurrentUserId();
        if (executorUserId == null) {
            throw new WarehouseException("USER_NOT_FOUND", "Current user ID is null");
        }
        
        BigDecimal purchasedQuantity = driverBalance.getQuantity();
        BigDecimal receivedQuantity = warehouseReceipt.getQuantity();
        BigDecimal totalDriverCost = driverBalance.getTotalCostEur();

        BigDecimal warehouseUnitPrice = calculator.calculateWarehouseUnitPrice(totalDriverCost, receivedQuantity);
        
        calculator.prepareWarehouseReceipt(warehouseReceipt, purchasedQuantity, warehouseUnitPrice, totalDriverCost, executorUserId);
        
        WarehouseReceipt savedReceipt = warehouseReceiptRepository.save(warehouseReceipt);
        
        LocalDate receiptDate = calculator.getReceiptDate(savedReceipt);
        
        if (purchasedQuantity.compareTo(receivedQuantity) != 0) {
            createDiscrepancyIfNeeded(savedReceipt, driverBalance, receivedQuantity, executorUserId, receiptDate);
        }

        balanceHandler.updateBalances(warehouseReceipt, purchasedQuantity, receivedQuantity, totalDriverCost);
        
        log.info("Warehouse receipt created: id={}", savedReceipt.getId());
        return savedReceipt;
    }

    private PageRequest createPageRequest(int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        return PageRequest.of(page, size, sortBy);
    }

    private void createDiscrepancyIfNeeded(
            @NonNull WarehouseReceipt savedReceipt,
            @NonNull DriverProductBalance driverBalance,
            @NonNull BigDecimal receivedQuantity,
            @NonNull Long executorUserId,
            @NonNull LocalDate receiptDate) {
        
        warehouseDiscrepancyService.createFromDriverBalance(
                savedReceipt.getId(),
                driverBalance,
                savedReceipt.getWarehouseId(),
                receiptDate,
                receivedQuantity,
                executorUserId,
                null
        );
    }
}
