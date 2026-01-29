package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.dto.impl.IdNameDTO;
import org.example.purchaseservice.services.purchase.PurchaseExportDataFetcher.FilterIds;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseFieldValueFormatter {
    
    private static final String FIELD_PREFIX = "field_";
    private static final String CLIENT_SUFFIX = "-client";
    private static final String UNKNOWN_PRODUCT = "Unknown Product";
    
    public String getFieldValue(@NonNull Purchase purchase, ClientDTO client, @NonNull String field, 
                                  @NonNull FilterIds filterIds, @NonNull List<ClientFieldValueDTO> fieldValues) {
        if (field.startsWith(FIELD_PREFIX)) {
            Long fieldId = parseFieldId(field);
            return fieldId != null ? getDynamicFieldValue(fieldValues, fieldId) : "";
        }
        
        if (field.endsWith(CLIENT_SUFFIX) && client != null) {
            return getClientFieldValue(client, field, filterIds);
        }
        
        return getPurchaseFieldValue(purchase, field, filterIds);
    }
    
    private String getClientFieldValue(@NonNull ClientDTO client, @NonNull String field, @NonNull FilterIds filterIds) {
        return switch (field) {
            case "id-client" -> client.getId() != null ? String.valueOf(client.getId()) : "";
            case "company-client" -> client.getCompany() != null ? client.getCompany() : "";
            case "createdAt-client" -> client.getCreatedAt() != null ? client.getCreatedAt() : "";
            case "updatedAt-client" -> client.getUpdatedAt() != null ? client.getUpdatedAt() : "";
            case "source-client" -> getClientSourceName(client, filterIds.sourceDTOs());
            default -> "";
        };
    }
    
    private String getClientSourceName(@NonNull ClientDTO client, @NonNull List<SourceDTO> sourceDTOs) {
        try {
            java.lang.reflect.Method getSourceMethod = client.getClass().getMethod("getSource");
            Object sourceObj = getSourceMethod.invoke(client);
            if (sourceObj != null) {
                java.lang.reflect.Method getNameMethod = sourceObj.getClass().getMethod("getName");
                Object sourceName = getNameMethod.invoke(sourceObj);
                if (sourceName != null) {
                    return sourceName.toString();
                }
            }
        } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException _) {
        }
        
        String sourceId = client.getSourceId();
        if (sourceId == null || sourceId.trim().isEmpty()) {
            return "";
        }
        try {
            Long sourceIdLong = Long.parseLong(sourceId.trim());
            return getNameFromDTOList(sourceDTOs, sourceIdLong);
        } catch (NumberFormatException e) {
            return "";
        }
    }
    
    private String getPurchaseFieldValue(@NonNull Purchase purchase, @NonNull String field, @NonNull FilterIds filterIds) {
        return switch (field) {
            case "id" -> purchase.getId() != null ? String.valueOf(purchase.getId()) : "";
            case "user" -> getNameFromDTOList(filterIds.userDTOs(), purchase.getUser());
            case "source" -> getNameFromDTOList(filterIds.sourceDTOs(), purchase.getSource());
            case "product" -> getProductName(purchase.getProduct(), filterIds.productDTOs());
            case "quantity" -> purchase.getQuantity() != null ? purchase.getQuantity().toString() : "";
            case "unitPrice" -> purchase.getUnitPrice() != null ? purchase.getUnitPrice().toString() : "";
            case "totalPrice" -> purchase.getTotalPrice() != null ? purchase.getTotalPrice().toString() : "";
            case "totalPriceEur" -> purchase.getTotalPriceEur() != null ? purchase.getTotalPriceEur().toString() : "";
            case "paymentMethod" -> formatPaymentMethod(purchase.getPaymentMethod());
            case "currency" -> purchase.getCurrency() != null ? purchase.getCurrency() : "";
            case "exchangeRate" -> purchase.getExchangeRate() != null ? String.valueOf(purchase.getExchangeRate()) : "";
            case "transaction" -> purchase.getTransaction() != null ? purchase.getTransaction().toString() : "";
            case "createdAt" -> purchase.getCreatedAt() != null ? purchase.getCreatedAt().toString() : "";
            case "updatedAt" -> purchase.getUpdatedAt() != null ? purchase.getUpdatedAt().toString() : "";
            case "comment" -> purchase.getComment() != null ? purchase.getComment() : "";
            default -> "";
        };
    }
    
    private String getProductName(Long productId, @NonNull List<Product> products) {
        if (productId == null) {
            return "";
        }
        return products.stream()
                .filter(product -> product != null && product.getId() != null && product.getId().equals(productId))
                .findFirst()
                .map(Product::getName)
                .orElse(UNKNOWN_PRODUCT);
    }
    
    private String formatPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "";
        }
        return paymentMethod == PaymentMethod.CASH ? "2" : "1";
    }
    
    private String getDynamicFieldValue(List<ClientFieldValueDTO> fieldValues, Long fieldId) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return "";
        }
        
        List<ClientFieldValueDTO> matchingValues = fieldValues.stream()
                .filter(fv -> fv.getFieldId() != null && fv.getFieldId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .toList();
        
        if (matchingValues.isEmpty()) {
            return "";
        }
        
        ClientFieldValueDTO firstValue = matchingValues.getFirst();
        String fieldType = firstValue.getFieldType();
        
        if (matchingValues.size() > 1) {
            return matchingValues.stream()
                    .map(fv -> formatFieldValue(fv, fieldType))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
        } else {
            return formatFieldValue(firstValue, fieldType);
        }
    }
    
    private String formatFieldValue(ClientFieldValueDTO fieldValue, String fieldType) {
        if (fieldValue == null) {
            return "";
        }
        
        return switch (fieldType) {
            case "TEXT", "PHONE" -> fieldValue.getValueText() != null ? fieldValue.getValueText() : "";
            case "NUMBER" -> fieldValue.getValueNumber() != null ? fieldValue.getValueNumber().toString() : "";
            case "DATE" -> fieldValue.getValueDate() != null ? fieldValue.getValueDate().toString() : "";
            case "BOOLEAN" -> {
                if (fieldValue.getValueBoolean() == null) yield "";
                yield fieldValue.getValueBoolean() ? "Так" : "Ні";
            }
            case "LIST" -> fieldValue.getValueListValue() != null ? fieldValue.getValueListValue() : "";
            default -> "";
        };
    }
    
    private <T extends IdNameDTO> String getNameFromDTOList(@NonNull List<T> dtoList, Long id) {
        if (id == null) {
            return "";
        }
        return dtoList.stream()
                .filter(dto -> dto != null && dto.getId() != null && dto.getId().equals(id))
                .findFirst()
                .map(IdNameDTO::getName)
                .orElse("");
    }
    
    public Long parseFieldId(String field) {
        try {
            return Long.parseLong(field.substring(FIELD_PREFIX.length()));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }
}
