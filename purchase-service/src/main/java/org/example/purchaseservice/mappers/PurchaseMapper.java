package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.purchase.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PurchaseMapper {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    public PurchaseDTO toDto(@NonNull Purchase purchase) {
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
        dto.setExchangeRate(purchase.getExchangeRate());
        dto.setTransactionId(purchase.getTransaction());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setUpdatedAt(purchase.getUpdatedAt());
        dto.setComment(purchase.getComment());
        return dto;
    }

    public PurchaseDTO toDtoForCreate(@NonNull Purchase purchase) {
        PurchaseDTO dto = toDto(purchase);
        dto.setIsReceived(false);
        return dto;
    }

    public Purchase purchaseUpdateDTOToPurchase(@NonNull PurchaseUpdateDTO purchaseDto) {
        Purchase purchase = new Purchase();
        purchase.setSource(purchaseDto.getSourceId());
        purchase.setProduct(purchaseDto.getProductId());
        purchase.setQuantity(purchaseDto.getQuantity());
        purchase.setTotalPrice(purchaseDto.getTotalPrice());
        purchase.setExchangeRate(purchaseDto.getExchangeRate());
        purchase.setCreatedAt(parseCreatedAt(purchaseDto.getCreatedAt()));
        purchase.setComment(purchaseDto.getComment());
        return purchase;
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(createdAt, DATE_FORMATTER);
                return localDate.atStartOfDay();
            } catch (Exception e) {
                throw new PurchaseException("INVALID_DATE_FORMAT",
                        String.format("Invalid date format for createdAt: %s", createdAt));
            }
        }
        return null;
    }

    public Purchase purchaseCreateDTOToPurchase(@NonNull PurchaseCreateDTO dto) {
        Purchase purchase = new Purchase();
        purchase.setUser(dto.getUserId());
        purchase.setClient(dto.getClientId());
        purchase.setSource(dto.getSourceId());
        purchase.setProduct(dto.getProductId());
        purchase.setQuantity(dto.getQuantity());
        purchase.setTotalPrice(dto.getTotalPrice());
        purchase.setPaymentMethod(dto.getPaymentMethod());
        purchase.setCurrency(dto.getCurrency());
        purchase.setExchangeRate(dto.getExchangeRate());
        purchase.setComment(dto.getComment());
        return purchase;
    }

    public PurchaseWarehouseDTO purchaseToPurchaseWarehouseDTO(@NonNull Purchase purchase) {
        PurchaseWarehouseDTO dto = new PurchaseWarehouseDTO();
        dto.setId(purchase.getId());
        dto.setUserId(purchase.getUser());
        dto.setProductId(purchase.getProduct());
        dto.setQuantity(purchase.getQuantity());
        dto.setCreatedAt(purchase.getCreatedAt());
        return dto;
    }

    public PurchaseModalDTO purchaseToPurchaseModalDTO(@NonNull Purchase purchase) {
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
        dto.setExchangeRate(purchase.getExchangeRate());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setComment(purchase.getComment());
        return dto;
    }

    public PurchasePageDTO toPurchasePageDTO(@NonNull Purchase purchase, ClientDTO client) {
        PurchasePageDTO dto = new PurchasePageDTO();
        dto.setId(purchase.getId());
        dto.setUserId(purchase.getUser());
        dto.setClient(client);
        dto.setSourceId(purchase.getSource());
        dto.setProductId(purchase.getProduct());
        dto.setQuantity(purchase.getQuantity());
        dto.setUnitPrice(purchase.getUnitPrice());
        dto.setTotalPrice(purchase.getTotalPrice());
        dto.setTotalPriceEur(purchase.getTotalPriceEur());
        dto.setPaymentMethod(purchase.getPaymentMethod());
        dto.setCurrency(purchase.getCurrency());
        dto.setExchangeRate(purchase.getExchangeRate());
        dto.setTransactionId(purchase.getTransaction());
        dto.setCreatedAt(purchase.getCreatedAt());
        dto.setUpdatedAt(purchase.getUpdatedAt());
        dto.setComment(purchase.getComment());
        return dto;
    }
}
