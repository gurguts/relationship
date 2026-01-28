package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.field.Source;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientFieldValueFormatter {

    private static final String FIELD_PREFIX = "field_";
    private static final String BOOLEAN_TRUE_UA = "Так";
    private static final String BOOLEAN_FALSE_UA = "Ні";
    private static final String EMPTY_STRING = "";

    private final ClientExportValidator validator;

    public String getFieldValue(@NonNull Client client, @NonNull String field, 
                                @NonNull Map<Long, Source> sourceMap) {
        if (field.startsWith(FIELD_PREFIX)) {
            return getDynamicFieldValue(client, field);
        }

        return switch (field) {
            case "id" -> client.getId() != null ? String.valueOf(client.getId()) : EMPTY_STRING;
            case "company" -> client.getCompany();
            case "createdAt" -> client.getCreatedAt() != null ? client.getCreatedAt().toString() : EMPTY_STRING;
            case "updatedAt" -> client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : EMPTY_STRING;
            case "source" -> getSourceName(client, sourceMap);
            default -> EMPTY_STRING;
        };
    }

    private String getSourceName(@NonNull Client client, @NonNull Map<Long, Source> sourceMap) {
        if (client.getSourceId() == null) {
            return EMPTY_STRING;
        }
        
        Source source = sourceMap.get(client.getSourceId());
        return source != null ? source.getName() : EMPTY_STRING;
    }

    private String getDynamicFieldValue(@NonNull Client client, @NonNull String field) {
        try {
            Long fieldId = validator.parseFieldIdFromString(field);
            if (fieldId == null) {
                return EMPTY_STRING;
            }
            return getDynamicFieldValueById(client, fieldId);
        } catch (Exception e) {
            log.warn("Failed to get dynamic field value for field {}: {}", field, e.getMessage());
            return EMPTY_STRING;
        }
    }

    private String getDynamicFieldValueById(@NonNull Client client, @NonNull Long fieldId) {
        if (client.getFieldValues() == null || client.getFieldValues().isEmpty()) {
            return EMPTY_STRING;
        }
        
        List<ClientFieldValue> fieldValues = findFieldValuesByFieldId(client, fieldId);
        
        if (fieldValues.isEmpty()) {
            return EMPTY_STRING;
        }
        
        ClientTypeField field = extractFieldType(fieldValues);

        return formatFieldValues(fieldValues, field);
    }

    private List<ClientFieldValue> findFieldValuesByFieldId(@NonNull Client client, @NonNull Long fieldId) {
        return client.getFieldValues().stream()
                .filter(fv -> fv.getField().getId() != null && fv.getField().getId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .collect(Collectors.toList());
    }

    private ClientTypeField extractFieldType(@NonNull List<ClientFieldValue> fieldValues) {
        ClientFieldValue firstValue = fieldValues.getFirst();
        return firstValue.getField();
    }

    private String formatFieldValues(@NonNull List<ClientFieldValue> fieldValues, @NonNull ClientTypeField field) {
        boolean allowMultiple = Boolean.TRUE.equals(field.getAllowMultiple());
        
        if (allowMultiple && fieldValues.size() > 1) {
            return formatMultipleFieldValues(fieldValues, field);
        } else {
            return formatFieldValue(fieldValues.getFirst(), field);
        }
    }

    private String formatMultipleFieldValues(@NonNull List<ClientFieldValue> fieldValues, @NonNull ClientTypeField field) {
        return fieldValues.stream()
                .map(fv -> formatFieldValue(fv, field))
                .filter(v -> !v.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String formatFieldValue(@NonNull ClientFieldValue fieldValue, @NonNull ClientTypeField field) {
        return switch (field.getFieldType()) {
            case TEXT, PHONE -> fieldValue.getValueText() != null ? fieldValue.getValueText() : EMPTY_STRING;
            case NUMBER -> fieldValue.getValueNumber() != null ? String.valueOf(fieldValue.getValueNumber()) : EMPTY_STRING;
            case DATE -> fieldValue.getValueDate() != null ? fieldValue.getValueDate().toString() : EMPTY_STRING;
            case BOOLEAN -> formatBooleanValue(fieldValue.getValueBoolean());
            case LIST -> formatListValue(fieldValue);
        };
    }

    private String formatBooleanValue(Boolean value) {
        if (value == null) {
            return EMPTY_STRING;
        }
        return value ? BOOLEAN_TRUE_UA : BOOLEAN_FALSE_UA;
    }

    private String formatListValue(@NonNull ClientFieldValue fieldValue) {
        if (fieldValue.getValueList() != null && fieldValue.getValueList().getValue() != null) {
            return fieldValue.getValueList().getValue();
        }
        return EMPTY_STRING;
    }
}
