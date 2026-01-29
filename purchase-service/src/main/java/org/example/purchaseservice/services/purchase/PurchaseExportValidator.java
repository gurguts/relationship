package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseExportValidator {
    
    private static final int MAX_QUERY_LENGTH = 255;
    private static final String FIELD_PREFIX = "field_";
    private static final String CLIENT_SUFFIX = "-client";
    
    private static final Set<String> VALID_PURCHASE_FIELDS = Set.of(
            "id", "user", "source", "product", "quantity", "unitPrice", "totalPrice", "totalPriceEur",
            "paymentMethod", "currency", "exchangeRate", "transaction", "createdAt", "updatedAt", "comment"
    );
    
    private static final Set<String> VALID_CLIENT_FIELDS = Set.of(
            "id-client", "company-client", "createdAt-client", "updatedAt-client", "source-client"
    );
    
    private static final Set<String> VALID_SORT_PROPERTIES = Set.of(
            "id", "user", "client", "source", "product", "quantity", "unitPrice", "totalPrice",
            "paymentMethod", "transaction", "createdAt", "updatedAt", "currency", "exchangeRate",
            "comment", "totalPriceEur", "unitPriceEur"
    );
    
    public void validateInputs(String query, @NonNull List<String> selectedFields) {
        validateQuery(query);
        validateSelectedFields(selectedFields);
    }
    
    public void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new PurchaseException("INVALID_QUERY", 
                    String.format("Search query cannot exceed %d characters", MAX_QUERY_LENGTH));
        }
    }
    
    public void validateSelectedFields(@NonNull List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            throw new PurchaseException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        
        for (String field : selectedFields) {
            if (field == null || field.trim().isEmpty()) {
                throw new PurchaseException("INVALID_FIELDS", 
                        "Selected fields cannot contain null or empty values");
            }
            
            String trimmedField = field.trim();
            if (!isValidField(trimmedField)) {
                throw new PurchaseException("INVALID_FIELDS", 
                        String.format("Invalid field name: %s", trimmedField));
            }
        }
    }
    
    public void validateFilterParams(Map<String, List<String>> filterParams) {
        if (filterParams == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new PurchaseException("INVALID_FILTER_PARAMS", 
                        "Filter parameter keys cannot be null or empty");
            }
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new PurchaseException("INVALID_FILTER_PARAMS", 
                                "Filter parameter values cannot be null or empty");
                    }
                }
            }
        }
    }
    
    public void validateSortProperty(@NonNull String sortProperty) {
        if (sortProperty.trim().isEmpty()) {
            throw new PurchaseException("INVALID_SORT_PROPERTY", "Sort property cannot be empty");
        }
        if (!VALID_SORT_PROPERTIES.contains(sortProperty)) {
            throw new PurchaseException("INVALID_SORT_PROPERTY", 
                    String.format("Invalid sort property: %s. Valid properties: %s", 
                            sortProperty, String.join(", ", VALID_SORT_PROPERTIES)));
        }
    }
    
    private boolean isValidField(@NonNull String field) {
        if (isDynamicField(field)) {
            return isValidDynamicField(field);
        }
        
        if (VALID_PURCHASE_FIELDS.contains(field) || VALID_CLIENT_FIELDS.contains(field)) {
            return true;
        }
        
        return isClientSuffixedField(field);
    }
    
    private boolean isDynamicField(@NonNull String field) {
        return field.startsWith(FIELD_PREFIX);
    }
    
    private boolean isValidDynamicField(@NonNull String field) {
        try {
            Long.parseLong(field.substring(FIELD_PREFIX.length()));
            return true;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return false;
        }
    }
    
    private boolean isClientSuffixedField(@NonNull String field) {
        if (!field.endsWith(CLIENT_SUFFIX)) {
            return false;
        }
        String baseField = field.substring(0, field.length() - CLIENT_SUFFIX.length());
        return VALID_PURCHASE_FIELDS.contains(baseField);
    }
}
