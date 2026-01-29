package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.utils.ValidationUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferValidator {
    
    private static final int MAX_PAGE_SIZE = 1000;
    
    public void validateTransferDTO(@NonNull ProductTransferDTO transferDTO) {
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
    
    public void validateQuantity(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new PurchaseException("INVALID_TRANSFER_QUANTITY", "Quantity cannot be negative");
        }
    }
    
    public void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new PurchaseException("INVALID_DATE_RANGE",
                    String.format("Date from (%s) cannot be after date to (%s)", dateFrom, dateTo));
        }
    }
    
    public void validatePage(int page) {
        if (page < 0) {
            throw new PurchaseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", page));
        }
    }
    
    public void validatePageSize(int size) {
        ValidationUtils.validatePageSize(size, MAX_PAGE_SIZE);
    }
    
    public void validateSameProductIds(@NonNull Long fromProductId, @NonNull Long toProductId) {
        if (fromProductId.equals(toProductId)) {
            throw new PurchaseException("INVALID_TRANSFER",
                    "From product ID and to product ID cannot be the same");
        }
    }
}
