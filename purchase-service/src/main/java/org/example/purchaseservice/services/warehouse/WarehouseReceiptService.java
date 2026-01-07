package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.dto.warehouse.WarehouseReceiptDTO;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.services.balance.DriverProductBalanceService;
import org.example.purchaseservice.services.balance.IWarehouseProductBalanceService;
import org.example.purchaseservice.services.impl.IWarehouseReceiptService;
import org.example.purchaseservice.spec.WarehouseReceiptSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.example.purchaseservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseReceiptService implements IWarehouseReceiptService {
    
    private static final int PRICE_SCALE = 6;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.HALF_UP;
    
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final DriverProductBalanceService driverProductBalanceService;
    private final IWarehouseProductBalanceService warehouseProductBalanceService;
    private final WarehouseDiscrepancyService warehouseDiscrepancyService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseReceiptDTO> getWarehouseReceipts(
            int page, 
            int size, 
            String sort,
            String direction, 
            Map<String, List<String>> filters) {
        
        validatePage(page);
        validatePageSize(size);
        validateSortParams(sort, direction);
        validateFilters(filters);
        
        PageRequest pageRequest = createPageRequest(page, size, sort, direction);
        WarehouseReceiptSpecification spec = new WarehouseReceiptSpecification(filters);
        Page<WarehouseReceipt> warehouseReceiptPage = warehouseReceiptRepository.findAll(spec, pageRequest);

        List<WarehouseReceiptDTO> content = warehouseReceiptPage.getContent().stream()
                .map(this::convertToDTO)
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
        validateWarehouseReceipt(warehouseReceipt);
        
        DriverProductBalance driverBalance = driverProductBalanceService.getBalance(
                warehouseReceipt.getUserId(), 
                warehouseReceipt.getProductId());
        
        validateDriverBalance(driverBalance, warehouseReceipt.getUserId(), warehouseReceipt.getProductId());
        
        Long executorUserId = SecurityUtils.getCurrentUserId();
        if (executorUserId == null) {
            throw new WarehouseException("USER_NOT_FOUND", "Current user ID is null");
        }
        
        BigDecimal purchasedQuantity = driverBalance.getQuantity();
        BigDecimal receivedQuantity = warehouseReceipt.getQuantity();
        BigDecimal totalDriverCost = driverBalance.getTotalCostEur();

        BigDecimal warehouseUnitPrice = calculateWarehouseUnitPrice(totalDriverCost, receivedQuantity);
        
        prepareWarehouseReceipt(warehouseReceipt, purchasedQuantity, warehouseUnitPrice, totalDriverCost, executorUserId);
        
        WarehouseReceipt savedReceipt = warehouseReceiptRepository.save(warehouseReceipt);
        
        LocalDate receiptDate = getReceiptDate(savedReceipt);
        
        if (purchasedQuantity.compareTo(receivedQuantity) != 0) {
            createDiscrepancyIfNeeded(savedReceipt, driverBalance, receivedQuantity, executorUserId, receiptDate);
        }

        updateBalances(warehouseReceipt, purchasedQuantity, receivedQuantity, totalDriverCost);
        
        return savedReceipt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseReceipt> findWarehouseReceiptsByFilters(@NonNull Map<String, List<String>> filters) {
        validateFilters(filters);
        Specification<WarehouseReceipt> spec = new WarehouseReceiptSpecification(filters);
        return warehouseReceiptRepository.findAll(spec);
    }

    private WarehouseReceiptDTO convertToDTO(@NonNull WarehouseReceipt receipt) {
        WarehouseReceiptDTO dto = new WarehouseReceiptDTO();
        dto.setId(receipt.getId());
        dto.setUserId(receipt.getUserId());
        dto.setProductId(receipt.getProductId());
        dto.setWarehouseId(receipt.getWarehouseId());
        dto.setQuantity(receipt.getQuantity());
        dto.setDriverBalanceQuantity(receipt.getDriverBalanceQuantity());
        dto.setEntryDate(receipt.getEntryDate());
        dto.setType(receipt.getType());
        dto.setUnitPriceEur(receipt.getUnitPriceEur());
        dto.setTotalCostEur(receipt.getTotalCostEur());
        dto.setPurchasedQuantity(receipt.getDriverBalanceQuantity());
        return dto;
    }

    private PageRequest createPageRequest(int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        return PageRequest.of(page, size, sortBy);
    }

    private void validateWarehouseReceipt(@NonNull WarehouseReceipt warehouseReceipt) {
        if (warehouseReceipt.getUserId() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "User ID cannot be null");
        }
        if (warehouseReceipt.getProductId() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "Product ID cannot be null");
        }
        if (warehouseReceipt.getWarehouseId() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "Warehouse ID cannot be null");
        }
        if (warehouseReceipt.getQuantity() == null) {
            throw new WarehouseException("INVALID_RECEIPT", "Quantity cannot be null");
        }
        if (warehouseReceipt.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WarehouseException("INVALID_RECEIPT", "Quantity must be positive");
        }
    }

    private void validateDriverBalance(DriverProductBalance driverBalance, Long userId, Long productId) {
        if (driverBalance == null) {
            throw new WarehouseException("INSUFFICIENT_DRIVER_BALANCE", 
                    String.format("Driver %d doesn't have product %d in balance", userId, productId));
        }
        if (driverBalance.getQuantity() == null) {
            throw new WarehouseException("INVALID_DRIVER_BALANCE", "Driver balance quantity cannot be null");
        }
        if (driverBalance.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new WarehouseException("INSUFFICIENT_DRIVER_BALANCE", 
                    String.format("Driver %d doesn't have product %d in balance", userId, productId));
        }
        if (driverBalance.getTotalCostEur() == null) {
            throw new WarehouseException("INVALID_DRIVER_BALANCE", "Driver balance total cost cannot be null");
        }
        if (driverBalance.getAveragePriceEur() == null) {
            throw new WarehouseException("INVALID_DRIVER_BALANCE", "Driver balance average price cannot be null");
        }
    }

    private BigDecimal calculateWarehouseUnitPrice(@NonNull BigDecimal totalDriverCost, @NonNull BigDecimal receivedQuantity) {
        if (receivedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WarehouseException("INVALID_QUANTITY", "Received quantity must be positive");
        }
        return totalDriverCost.divide(receivedQuantity, PRICE_SCALE, PRICE_ROUNDING_MODE);
    }

    private void prepareWarehouseReceipt(
            @NonNull WarehouseReceipt warehouseReceipt,
            @NonNull BigDecimal purchasedQuantity,
            @NonNull BigDecimal warehouseUnitPrice,
            @NonNull BigDecimal totalDriverCost,
            @NonNull Long executorUserId) {
        
        warehouseReceipt.setDriverBalanceQuantity(purchasedQuantity);
        warehouseReceipt.setUnitPriceEur(warehouseUnitPrice);
        warehouseReceipt.setTotalCostEur(totalDriverCost);
        warehouseReceipt.setExecutorUserId(executorUserId);
        
        if (warehouseReceipt.getEntryDate() == null) {
            warehouseReceipt.setEntryDate(LocalDate.now());
        }
    }

    private LocalDate getReceiptDate(@NonNull WarehouseReceipt savedReceipt) {
        if (savedReceipt.getEntryDate() != null) {
            return savedReceipt.getEntryDate();
        }
        if (savedReceipt.getCreatedAt() != null) {
            return savedReceipt.getCreatedAt().toLocalDate();
        }
        return LocalDate.now();
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

    private void updateBalances(
            @NonNull WarehouseReceipt warehouseReceipt,
            @NonNull BigDecimal purchasedQuantity,
            @NonNull BigDecimal receivedQuantity,
            @NonNull BigDecimal totalDriverCost) {
        
        driverProductBalanceService.removeProduct(
                warehouseReceipt.getUserId(),
                warehouseReceipt.getProductId(),
                purchasedQuantity,
                totalDriverCost
        );

        warehouseProductBalanceService.addProduct(
                warehouseReceipt.getWarehouseId(),
                warehouseReceipt.getProductId(),
                receivedQuantity,
                totalDriverCost
        );
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new WarehouseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", page));
        }
    }

    private void validatePageSize(int size) {
        if (size <= 0) {
            throw new WarehouseException("INVALID_PAGE_SIZE", "Page size must be positive");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new WarehouseException("INVALID_PAGE_SIZE",
                    String.format("Page size cannot exceed %d, got: %d", MAX_PAGE_SIZE, size));
        }
    }

    private void validateSortParams(String sort, String direction) {
        if (sort == null || sort.trim().isEmpty()) {
            throw new WarehouseException("INVALID_SORT", "Sort parameter cannot be null or empty");
        }
        if (direction == null || direction.trim().isEmpty()) {
            throw new WarehouseException("INVALID_SORT_DIRECTION", "Sort direction cannot be null or empty");
        }
        try {
            Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new WarehouseException("INVALID_SORT_DIRECTION",
                    String.format("Invalid sort direction: %s. Valid values: ASC, DESC", direction));
        }
    }

    private void validateFilters(Map<String, List<String>> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new WarehouseException("INVALID_FILTER", "Filter keys cannot be null or empty");
            }
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new WarehouseException("INVALID_FILTER", "Filter values cannot be null or empty");
                    }
                }
            }
        }
    }

}
