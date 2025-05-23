package org.example.purchaseservice.mappers;

import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PurchaseMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PurchaseDTO toDto(Purchase purchase) {
        if (purchase == null) {
            return null;
        }
        PurchaseDTO dto = new PurchaseDTO();
        dto.setId(purchase.getId());
        dto.setUserId(purchase.getUser());
        dto.setClientId(purchase.getClient());
        dto.setSourceId(purchase.getSource());
        dto.setProductId(purchase.getProduct());
        dto.setQuantity(purchase.getQuantity());
        dto.setUnitPrice(purchase.getUnitPrice());
        dto.setTotalPrice(purchase.getTotalPrice());
        dto.setPaymentMethod(purchase.getPaymentMethod());
        dto.setCurrency(purchase.getCurrency());
        dto.setTransactionId(purchase.getTransaction());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setUpdatedAt(purchase.getUpdatedAt());
        return dto;
    }

    public Purchase purchaseUpdateDTOToPurchase(PurchaseUpdateDTO purchaseDto) {
        if (purchaseDto == null) {
            return null;
        }
        Purchase purchase = new Purchase();
        purchase.setSource(purchaseDto.getSourceId());
        purchase.setProduct(purchaseDto.getProductId());
        purchase.setQuantity(purchaseDto.getQuantity());
        purchase.setTotalPrice(purchaseDto.getTotalPrice());
        purchase.setCreatedAt(parseCreatedAt(purchaseDto.getCreatedAt()));

        return purchase;
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(createdAt, DATE_FORMATTER);
                return localDate.atStartOfDay();
            } catch (Exception e) {
                throw new PurchaseException(String.format("Invalid date format for createdAt: %s", createdAt));
            }
        }
        return null;
    }

    public Purchase purchaseCreateDTOToPurchase(PurchaseCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Purchase purchase = new Purchase();
        purchase.setClient(dto.getClientId());
        purchase.setSource(dto.getSourceId());
        purchase.setProduct(dto.getProductId());
        purchase.setQuantity(dto.getQuantity());
        purchase.setTotalPrice(dto.getTotalPrice());
        purchase.setPaymentMethod(dto.getPaymentMethod());
        purchase.setCurrency(dto.getCurrency());
        return purchase;
    }

    public PurchaseWarehouseDTO purchaseToPurchaseWarehouseDTO(Purchase purchase) {
        if (purchase == null) {
            return null;
        }

        PurchaseWarehouseDTO dto = new PurchaseWarehouseDTO();
        dto.setId(purchase.getId());
        dto.setUserId(purchase.getUser());
        dto.setProductId(purchase.getProduct());
        dto.setQuantity(purchase.getQuantity());
        dto.setCreatedAt(purchase.getCreatedAt());
        return dto;
    }

    public PurchaseModalDTO purchaseToPurchaseModalDTO(Purchase purchase) {
        if (purchase == null) {
            return null;
        }

        PurchaseModalDTO dto = new PurchaseModalDTO();
        dto.setId(purchase.getId());
        dto.setUserId(purchase.getUser());
        dto.setSourceId(purchase.getSource());
        dto.setProductId(purchase.getProduct());
        dto.setQuantity(purchase.getQuantity());
        dto.setUnitPrice(purchase.getUnitPrice());
        dto.setTotalPrice(purchase.getTotalPrice());
        dto.setPaymentMethod(purchase.getPaymentMethod());
        dto.setCurrency(purchase.getCurrency());
        dto.setCreatedAt(purchase.getCreatedAt());
        return dto;
    }
}
