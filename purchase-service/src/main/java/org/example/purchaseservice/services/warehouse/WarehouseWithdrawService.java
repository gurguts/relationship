package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.balance.WarehouseProductBalance;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
import org.example.purchaseservice.spec.WarehouseWithdrawalSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawService implements IWarehouseWithdrawService {
    private final WarehouseWithdrawalRepository warehouseWithdrawalRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final org.example.purchaseservice.services.balance.WarehouseProductBalanceService warehouseProductBalanceService;
    private final org.example.purchaseservice.services.balance.ShipmentService shipmentService;


    @Override
    @Transactional
    public WarehouseWithdrawal createWithdrawal(WarehouseWithdrawal warehouseWithdrawal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();

        warehouseWithdrawal.setUserId(userId);
        BigDecimal quantity = warehouseWithdrawal.getQuantity().setScale(2, RoundingMode.HALF_UP);
        warehouseWithdrawal.setQuantity(quantity);

        // Check product availability on warehouse and get average price
        if (!warehouseProductBalanceService.hasEnoughProduct(
                warehouseWithdrawal.getWarehouseId(),
                warehouseWithdrawal.getProductId(),
                quantity)) {
            
            WarehouseProductBalance balance =
                    warehouseProductBalanceService.getBalance(
                            warehouseWithdrawal.getWarehouseId(),
                            warehouseWithdrawal.getProductId());
            
            throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                    "Insufficient product on warehouse. Available: %s, requested: %s",
                    balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                    quantity));
        }

        // Remove product from warehouse and get average price
        BigDecimal averagePrice = warehouseProductBalanceService.removeProduct(
                warehouseWithdrawal.getWarehouseId(),
                warehouseWithdrawal.getProductId(),
                quantity
        ).setScale(6, RoundingMode.HALF_UP);

        // Calculate withdrawal cost
        BigDecimal totalCost = quantity.multiply(averagePrice).setScale(6, RoundingMode.HALF_UP);
        
        warehouseWithdrawal.setUnitPriceEur(averagePrice);
        warehouseWithdrawal.setTotalCostEur(totalCost);

        WarehouseWithdrawal savedWithdrawal = warehouseWithdrawalRepository.save(warehouseWithdrawal);

        // If shipmentId is specified, update total vehicle cost
        if (warehouseWithdrawal.getShipmentId() != null) {
            shipmentService.addWithdrawalCost(warehouseWithdrawal.getShipmentId(), totalCost);
        }

        log.info("Warehouse withdrawal created: product removed from warehouse {}. Quantity: {}, Unit price: {}, Total cost: {}", 
                warehouseWithdrawal.getWarehouseId(), 
                warehouseWithdrawal.getQuantity(), 
                averagePrice, 
                totalCost);

        return savedWithdrawal;
    }

    @Override
    @Transactional
    public WarehouseWithdrawal updateWithdrawal(Long id, WarehouseWithdrawal request) {
        WarehouseWithdrawal withdrawal = warehouseWithdrawalRepository.findById(id)
                .orElseThrow(() -> new PurchaseException("WITHDRAWAL_NOT_FOUND", "Withdrawal not found"));

        if (request.getProductId() != null && !request.getProductId().equals(withdrawal.getProductId())) {
            throw new PurchaseException("PRODUCT_CHANGE_NOT_ALLOWED", "Cannot change product for existing withdrawal");
        }

        BigDecimal originalQuantity = withdrawal.getQuantity().setScale(2, RoundingMode.HALF_UP);
        withdrawal.setQuantity(originalQuantity);
        BigDecimal unitPrice = resolveUnitPrice(withdrawal);

        BigDecimal newQuantity = request.getQuantity() != null
                ? request.getQuantity().setScale(2, RoundingMode.HALF_UP)
                : originalQuantity;

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_QUANTITY", "Quantity cannot be negative");
        }

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Quantity set to 0 for withdrawal {}, returning products to warehouse and deleting record", id);
            restoreWithdrawalToWarehouse(withdrawal);
            warehouseWithdrawalRepository.delete(withdrawal);
            return null;
        }

        BigDecimal delta = newQuantity.subtract(originalQuantity);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            if (!warehouseProductBalanceService.hasEnoughProduct(withdrawal.getWarehouseId(), withdrawal.getProductId(), delta)) {
                WarehouseProductBalance balance =
                        warehouseProductBalanceService.getBalance(withdrawal.getWarehouseId(), withdrawal.getProductId());

                throw new PurchaseException("INSUFFICIENT_PRODUCT", String.format(
                        "Insufficient product on warehouse. Available: %s, requested additionally: %s",
                        balance != null ? balance.getQuantity() : BigDecimal.ZERO,
                        delta));
            }

            BigDecimal additionalQuantity = delta.setScale(2, RoundingMode.HALF_UP);
            BigDecimal additionalCost = unitPrice.multiply(additionalQuantity).setScale(6, RoundingMode.HALF_UP);

            warehouseProductBalanceService.removeProductWithCost(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    additionalQuantity,
                    additionalCost
            );

            if (withdrawal.getShipmentId() != null) {
                shipmentService.addWithdrawalCost(withdrawal.getShipmentId(), additionalCost);
            }

            log.info("Withdrawal {} increased by {} (cost {})", id, additionalQuantity, additionalCost);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal quantityToReturn = delta.abs().setScale(2, RoundingMode.HALF_UP);
            BigDecimal costToReturn = unitPrice.multiply(quantityToReturn).setScale(6, RoundingMode.HALF_UP);

            warehouseProductBalanceService.addProduct(
                    withdrawal.getWarehouseId(),
                    withdrawal.getProductId(),
                    quantityToReturn,
                    costToReturn
            );

            if (withdrawal.getShipmentId() != null) {
                shipmentService.subtractWithdrawalCost(withdrawal.getShipmentId(), costToReturn);
            }

            log.info("Withdrawal {} decreased by {} (return cost {})", id, quantityToReturn, costToReturn);
        } else {
            log.debug("Withdrawal {} quantity unchanged", id);
        }

        if (request.getWithdrawalReason() != null) {
            if (request.getWithdrawalReason().getPurpose() != WithdrawalReason.Purpose.REMOVING) {
                throw new PurchaseException("INVALID_WITHDRAWAL_REASON", "Withdrawal reason must have REMOVING purpose");
            }
            withdrawal.setWithdrawalReason(request.getWithdrawalReason());
        }
        if (request.getDescription() != null) {
            withdrawal.setDescription(request.getDescription());
        }
        if (request.getWithdrawalDate() != null) {
            withdrawal.setWithdrawalDate(request.getWithdrawalDate());
        }

        withdrawal.setQuantity(newQuantity);
        withdrawal.setUnitPriceEur(unitPrice);
        withdrawal.setTotalCostEur(calculateTotalCost(unitPrice, newQuantity));

        return warehouseWithdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional
    public void deleteWithdrawal(Long id) {
        WarehouseWithdrawal withdrawal = warehouseWithdrawalRepository.findById(id)
                .orElseThrow(() -> new PurchaseException("WITHDRAWAL_NOT_FOUND", "Withdrawal not found"));

        restoreWithdrawalToWarehouse(withdrawal);
        warehouseWithdrawalRepository.delete(withdrawal);
    }

    @Override
    public PageResponse<WithdrawalDTO> getWithdrawals(int page, int size, String sort, String direction,
                                                      Map<String, List<String>> filters) {
        Sort sortOrder = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageRequest = PageRequest.of(page, size, sortOrder);
        WarehouseWithdrawalSpecification spec = new WarehouseWithdrawalSpecification(filters);
        Page<WarehouseWithdrawal> withdrawalPage = warehouseWithdrawalRepository.findAll(spec, pageRequest);

        List<WithdrawalDTO> content = withdrawalPage.getContent().stream()
                .map(w -> WithdrawalDTO.builder()
                        .id(w.getId())
                        .productId(w.getProductId())
                        .warehouseId(w.getWarehouseId())
                        .userId(w.getUserId())
                        .withdrawalReason(w.getWithdrawalReason())
                        .quantity(w.getQuantity().doubleValue())
                        .unitPriceEur(w.getUnitPriceEur())
                        .totalCostEur(w.getTotalCostEur())
                        .description(w.getDescription())
                        .withdrawalDate(w.getWithdrawalDate())
                        .createdAt(w.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageResponse<>(
                withdrawalPage.getNumber(),
                withdrawalPage.getSize(),
                withdrawalPage.getTotalElements(),
                withdrawalPage.getTotalPages(),
                content
        );
    }

    @Override
    public List<WithdrawalReason> getAllWithdrawalReasons() {
        return withdrawalReasonRepository.findAll();
    }

    @Override
    public List<WithdrawalReason> getWithdrawalReasonsByPurpose(WithdrawalReason.Purpose purpose) {
        return withdrawalReasonRepository.findByPurpose(purpose);
    }

    private BigDecimal resolveUnitPrice(WarehouseWithdrawal withdrawal) {
        BigDecimal unitPrice = withdrawal.getUnitPriceEur();
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return unitPrice.setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal quantity = withdrawal.getQuantity();
        BigDecimal totalCost = withdrawal.getTotalCostEur();
        if (quantity != null
                && quantity.compareTo(BigDecimal.ZERO) > 0
                && totalCost != null
                && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return totalCost.divide(quantity, 6, RoundingMode.HALF_UP);
        }

        throw new PurchaseException("WITHDRAWAL_PRICE_MISSING",
                String.format("Withdrawal %d does not contain price information", withdrawal.getId()));
    }

    private BigDecimal calculateTotalCost(BigDecimal unitPrice, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return unitPrice.multiply(quantity).setScale(6, RoundingMode.HALF_UP);
    }

    private void restoreWithdrawalToWarehouse(WarehouseWithdrawal withdrawal) {
        BigDecimal quantity = withdrawal.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        quantity = quantity.setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitPrice = resolveUnitPrice(withdrawal);
        BigDecimal totalCost = calculateTotalCost(unitPrice, quantity);

        warehouseProductBalanceService.addProduct(
                withdrawal.getWarehouseId(),
                withdrawal.getProductId(),
                quantity,
                totalCost
        );

        if (withdrawal.getShipmentId() != null) {
            shipmentService.subtractWithdrawalCost(withdrawal.getShipmentId(), totalCost);
        }

        log.info("Returned withdrawal {} to warehouse {}: quantity={}, totalCost={}",
                withdrawal.getId(), withdrawal.getWarehouseId(), quantity, totalCost);
    }
}
