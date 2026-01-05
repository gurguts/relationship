package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.repositories.WarehouseReceiptRepository;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
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
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseReceiptService implements IWarehouseReceiptService {
    private final WarehouseReceiptRepository warehouseReceiptRepository;
    private final WarehouseWithdrawalRepository warehouseWithdrawalRepository;
    private final org.example.purchaseservice.services.balance.DriverProductBalanceService driverProductBalanceService;
    private final org.example.purchaseservice.services.balance.IWarehouseProductBalanceService warehouseProductBalanceService;
    private final WarehouseDiscrepancyService warehouseDiscrepancyService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseReceiptDTO> getWarehouseReceipts(int page, int size, String sort,
                                                               String direction, Map<String, List<String>> filters) {

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
    
    private WarehouseReceiptDTO convertToDTO(WarehouseReceipt receipt) {
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
        // purchasedQuantity is deprecated, can use driverBalanceQuantity instead
        dto.setPurchasedQuantity(receipt.getDriverBalanceQuantity());
        return dto;
    }

    private PageRequest createPageRequest(int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        return PageRequest.of(page, size, sortBy);
    }

    @Override
    @Transactional
    public WarehouseReceipt createWarehouseReceipt(WarehouseReceipt warehouseReceipt) {
        // Get driver balance for this product
        org.example.purchaseservice.models.balance.DriverProductBalance driverBalance = 
                driverProductBalanceService.getBalance(warehouseReceipt.getUserId(), warehouseReceipt.getProductId());
        
        if (driverBalance == null || driverBalance.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new WarehouseException("INSUFFICIENT_DRIVER_BALANCE", 
                    String.format("Driver %d doesn't have product %d in balance",
                            warehouseReceipt.getUserId(),
                            warehouseReceipt.getProductId()));
        }
        
        // Get current executor user ID
        Long executorUserId = SecurityUtils.getCurrentUserId();
        
        // Store driver's full balance details (before removing)
        BigDecimal purchasedQuantity = driverBalance.getQuantity();        // What driver bought
        BigDecimal receivedQuantity = warehouseReceipt.getQuantity();      // What warehouse clerk received
        BigDecimal totalDriverCost = driverBalance.getTotalCostEur();      // Total cost of all driver's product in EUR
        BigDecimal driverUnitPrice = driverBalance.getAveragePriceEur();   // Average price per unit from driver in EUR
        
        // Calculate warehouse unit price (total cost distributed over received quantity)
        // Round up to avoid loss of precision when converting back to total cost
        // If received 19kg for 400 EUR total → 400/19 = 21.052632... → 21.052633 EUR/kg
        BigDecimal warehouseUnitPrice = totalDriverCost.divide(receivedQuantity, 6, java.math.RoundingMode.CEILING);
        
        // Set prices and driver balance in receipt
        warehouseReceipt.setDriverBalanceQuantity(purchasedQuantity);  // Store how much driver had
        warehouseReceipt.setUnitPriceEur(warehouseUnitPrice);  // Recalculated price for received quantity in EUR
        warehouseReceipt.setTotalCostEur(totalDriverCost);     // Full driver cost goes to warehouse in EUR
        warehouseReceipt.setExecutorUserId(executorUserId);
        
        // Set entryDate to current date if not already set (will be set from createdAt after save)
        if (warehouseReceipt.getEntryDate() == null) {
            warehouseReceipt.setEntryDate(LocalDate.now());
        }
        
        // Always create new receipt - never merge with existing ones
        // This allows tracking multiple deliveries per day from the same driver
        WarehouseReceipt savedReceipt = warehouseReceiptRepository.save(warehouseReceipt);
        
        // Update entryDate from createdAt after save to ensure consistency
        if (savedReceipt.getCreatedAt() != null) {
            savedReceipt.setEntryDate(savedReceipt.getCreatedAt().toLocalDate());
            savedReceipt = warehouseReceiptRepository.save(savedReceipt);
        }
        
        // Create discrepancy record if received quantity differs from purchased quantity
        if (purchasedQuantity.compareTo(receivedQuantity) != 0) {
            warehouseDiscrepancyService.createFromDriverBalance(
                    savedReceipt.getId(),
                    driverBalance,
                    warehouseReceipt.getWarehouseId(),
                    savedReceipt.getEntryDate() != null ? savedReceipt.getEntryDate() : savedReceipt.getCreatedAt().toLocalDate(),
                    receivedQuantity,
                    executorUserId,
                    null  // Could add comment from receipt if needed
            );
            
            log.info("Discrepancy detected: Driver {} purchased {} kg but warehouse received {} kg", 
                    warehouseReceipt.getUserId(), purchasedQuantity, receivedQuantity);
        }
        
        // Remove ALL product from driver balance (clear driver's balance for this product)
        driverProductBalanceService.removeProduct(
                warehouseReceipt.getUserId(),
                warehouseReceipt.getProductId(),
                purchasedQuantity,      // Remove all purchased quantity
                totalDriverCost         // Remove all cost
        );
        
        // Add RECEIVED quantity to warehouse with TOTAL cost (no rounding errors)
        // Full driver cost is added directly, average price calculated from it
        // Example: 9 kg received for 300 UAH total → avg price = 300/9 = 33.33 UAH/kg
        warehouseProductBalanceService.addProduct(
                warehouseReceipt.getWarehouseId(),
                warehouseReceipt.getProductId(),
                receivedQuantity,        // Add received quantity
                totalDriverCost          // Total cost (exact amount, no rounding)
        );
        
        log.info("Warehouse receipt created: Driver {} balance cleared ({} kg for {} UAH @ {} UAH/kg). Warehouse {} received {} kg @ {} UAH/kg (total {} UAH)", 
                warehouseReceipt.getUserId(), purchasedQuantity, totalDriverCost, driverUnitPrice,
                warehouseReceipt.getWarehouseId(), receivedQuantity, warehouseUnitPrice, totalDriverCost);
        
        return savedReceipt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseReceipt> findWarehouseReceiptsByFilters(Map<String, List<String>> filters) {
        Specification<WarehouseReceipt> spec = new WarehouseReceiptSpecification(filters);
        return warehouseReceiptRepository.findAll(spec);
    }


    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, Double>> getWarehouseBalance(LocalDate balanceDate) {
        try {

            Map<Long, Map<Long, BigDecimal>> totalWarehouseReceipts = calculateTotalReceipts(balanceDate);
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals = calculateTotalWithdrawals(balanceDate);
            return Collections.unmodifiableMap(calculateBalance(totalWarehouseReceipts, totalWithdrawals));

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new WarehouseException("UNABLE_CALCULATE_WAREHOUSE_BALANCES", e.getMessage());
        }
    }

    private <T> Map<Long, Map<Long, BigDecimal>> calculateTotals(
            List<T> records,
            Function<T, Long> warehouseIdExtractor,
            Function<T, Long> productIdExtractor,
            Function<T, BigDecimal> quantityExtractor) {
        return records.stream()
                .filter(record -> isValidRecord(record, warehouseIdExtractor, productIdExtractor, quantityExtractor))
                .collect(Collectors.groupingBy(
                        warehouseIdExtractor,
                        Collectors.groupingBy(
                                productIdExtractor,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        quantityExtractor,
                                        BigDecimal::add
                                )
                        )
                ));
    }

    private <T> boolean isValidRecord(T record, Function<T, Long> warehouseIdExtractor,
                                      Function<T, Long> productIdExtractor, Function<T, BigDecimal> quantityExtractor) {
        if (warehouseIdExtractor.apply(record) == null ||
                productIdExtractor.apply(record) == null ||
                quantityExtractor.apply(record) == null) {
            log.warn("Invalid record detected: {}", record);
            return false;
        }
        return true;
    }

    private Map<Long, Map<Long, BigDecimal>> calculateTotalReceipts(LocalDate balanceDate) {
        List<WarehouseReceipt> receipts = warehouseReceiptRepository.findAllByEntryDateLessThanEqual(balanceDate);
        return calculateTotals(
                receipts,
                WarehouseReceipt::getWarehouseId,
                WarehouseReceipt::getProductId,
                WarehouseReceipt::getQuantity
        );
    }

    private Map<Long, Map<Long, BigDecimal>> calculateTotalWithdrawals(LocalDate balanceDate) {
        List<WarehouseWithdrawal> withdrawals = warehouseWithdrawalRepository.findAllByWithdrawalDateLessThanEqual(balanceDate);
        return calculateTotals(
                withdrawals,
                WarehouseWithdrawal::getWarehouseId,
                WarehouseWithdrawal::getProductId,
                WarehouseWithdrawal::getQuantity
        );
    }

    private Map<Long, Map<Long, Double>> calculateBalance(
            Map<Long, Map<Long, BigDecimal>> totalWarehouseReceipts,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Map<Long, Map<Long, Double>> balanceByWarehouseAndProduct = new HashMap<>();
        Set<Long> allWarehouseIds = getAllWarehouseIds(totalWarehouseReceipts, totalWithdrawals);

        for (Long warehouseId : allWarehouseIds) {
            balanceByWarehouseAndProduct.computeIfAbsent(warehouseId, _ -> new HashMap<>())
                    .putAll(calculateBalanceForWarehouse(warehouseId, totalWarehouseReceipts, totalWithdrawals));
        }

        return balanceByWarehouseAndProduct;
    }

    private Set<Long> getAllWarehouseIds(
            Map<Long, Map<Long, BigDecimal>> totalWarehouseReceipts,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Set<Long> allWarehouseIds = new HashSet<>();
        allWarehouseIds.addAll(totalWarehouseReceipts.keySet());
        allWarehouseIds.addAll(totalWithdrawals.keySet());
        return allWarehouseIds;
    }

    private Map<Long, Double> calculateBalanceForWarehouse(
            Long warehouseId,
            Map<Long, Map<Long, BigDecimal>> totalWarehouseReceipts,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Map<Long, Double> balanceByProduct = new HashMap<>();
        Set<Long> allProductIds = getAllProductIds(warehouseId, totalWarehouseReceipts, totalWithdrawals);

        for (Long productId : allProductIds) {
            BigDecimal receipts = totalWarehouseReceipts.getOrDefault(warehouseId, Collections.emptyMap())
                    .getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal withdrawals = totalWithdrawals.getOrDefault(warehouseId, Collections.emptyMap())
                    .getOrDefault(productId, BigDecimal.ZERO);
            balanceByProduct.put(productId, receipts.subtract(withdrawals).doubleValue());
        }

        return balanceByProduct;
    }

    private Set<Long> getAllProductIds(
            Long warehouseId,
            Map<Long, Map<Long, BigDecimal>> totalWarehouseReceipts,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Set<Long> allProductIds = new HashSet<>();
        if (totalWarehouseReceipts.containsKey(warehouseId)) {
            allProductIds.addAll(totalWarehouseReceipts.get(warehouseId).keySet());
        }
        if (totalWithdrawals.containsKey(warehouseId)) {
            allProductIds.addAll(totalWithdrawals.get(warehouseId).keySet());
        }
        return allProductIds;
    }
}


