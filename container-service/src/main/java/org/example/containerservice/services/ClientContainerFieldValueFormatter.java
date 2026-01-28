package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.containerservice.models.dto.fields.SourceDTO;
import org.example.containerservice.models.dto.impl.IdNameDTO;
import org.example.containerservice.services.ClientContainerExportDataFetcher.FilterIds;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientContainerFieldValueFormatter {

    private static final String FIELD_PREFIX = "field_";
    private static final int FIELD_PREFIX_LENGTH = 6;
    private static final String FIELD_SUFFIX_CLIENT = "-client";
    private static final String FIELD_TYPE_TEXT = "TEXT";
    private static final String FIELD_TYPE_PHONE = "PHONE";
    private static final String FIELD_TYPE_NUMBER = "NUMBER";
    private static final String FIELD_TYPE_DATE = "DATE";
    private static final String FIELD_TYPE_BOOLEAN = "BOOLEAN";
    private static final String FIELD_TYPE_LIST = "LIST";
    private static final String BOOLEAN_TRUE_UA = "Так";
    private static final String BOOLEAN_FALSE_UA = "Ні";
    private static final String EMPTY_STRING = "";
    private static final String VALUE_SEPARATOR = ", ";
    private static final String FIELD_ID_CLIENT = "id-client";
    private static final String FIELD_COMPANY_CLIENT = "company-client";
    private static final String FIELD_CREATED_AT_CLIENT = "createdAt-client";
    private static final String FIELD_UPDATED_AT_CLIENT = "updatedAt-client";
    private static final String FIELD_SOURCE_CLIENT = "source-client";
    private static final String FIELD_ID = "id";
    private static final String FIELD_USER = "user";
    private static final String FIELD_CONTAINER = "container";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    public String getFieldValue(@NonNull ClientContainer clientContainer,
                               ClientDTO client,
                               @NonNull String field,
                               @NonNull FilterIds filterIds,
                               @NonNull List<ClientFieldValueDTO> fieldValues) {
        if (field.startsWith(FIELD_PREFIX)) {
            Long fieldId = parseFieldId(field);
            if (fieldId != null) {
                return getDynamicFieldValue(fieldValues, fieldId);
            }
            return EMPTY_STRING;
        }

        if (field.endsWith(FIELD_SUFFIX_CLIENT) && client != null) {
            return getClientFieldValue(client, field, filterIds);
        }

        return getContainerFieldValue(clientContainer, field, filterIds);
    }

    private String getClientFieldValue(@NonNull ClientDTO client, @NonNull String field, @NonNull FilterIds filterIds) {
        return switch (field) {
            case FIELD_ID_CLIENT -> client.getId() != null ? String.valueOf(client.getId()) : EMPTY_STRING;
            case FIELD_COMPANY_CLIENT -> client.getCompany() != null ? client.getCompany() : EMPTY_STRING;
            case FIELD_CREATED_AT_CLIENT -> client.getCreatedAt() != null ? client.getCreatedAt() : EMPTY_STRING;
            case FIELD_UPDATED_AT_CLIENT -> client.getUpdatedAt() != null ? client.getUpdatedAt() : EMPTY_STRING;
            case FIELD_SOURCE_CLIENT -> getClientSourceName(client, filterIds.sourceDTOs());
            default -> EMPTY_STRING;
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
            return EMPTY_STRING;
        }
        try {
            Long sourceIdLong = Long.parseLong(sourceId.trim());
            return getNameFromDTOList(sourceDTOs, sourceIdLong);
        } catch (NumberFormatException e) {
            return EMPTY_STRING;
        }
    }

    private String getContainerFieldValue(@NonNull ClientContainer clientContainer,
                                         @NonNull String field,
                                         @NonNull FilterIds filterIds) {
        return switch (field) {
            case FIELD_ID -> clientContainer.getId() != null ? String.valueOf(clientContainer.getId()) : EMPTY_STRING;
            case FIELD_USER -> getNameFromDTOList(filterIds.userDTOs(), clientContainer.getUser());
            case FIELD_CONTAINER -> clientContainer.getContainer().getName();
            case FIELD_QUANTITY -> clientContainer.getQuantity() != null
                    ? clientContainer.getQuantity().toString() : EMPTY_STRING;
            case FIELD_UPDATED_AT -> clientContainer.getUpdatedAt() != null
                    ? clientContainer.getUpdatedAt().toString() : EMPTY_STRING;
            default -> EMPTY_STRING;
        };
    }

    private String getDynamicFieldValue(@NonNull List<ClientFieldValueDTO> fieldValues, @NonNull Long fieldId) {
        if (fieldValues.isEmpty()) {
            return EMPTY_STRING;
        }

        List<ClientFieldValueDTO> matchingValues = fieldValues.stream()
                .filter(fv -> fv.getFieldId() != null && fv.getFieldId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .toList();

        if (matchingValues.isEmpty()) {
            return EMPTY_STRING;
        }

        ClientFieldValueDTO firstValue = matchingValues.getFirst();
        String fieldType = firstValue.getFieldType();

        if (matchingValues.size() > 1) {
            return matchingValues.stream()
                    .map(fv -> formatFieldValue(fv, fieldType))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(VALUE_SEPARATOR));
        }

        return formatFieldValue(firstValue, fieldType);
    }

    private String formatFieldValue(@NonNull ClientFieldValueDTO fieldValue, @NonNull String fieldType) {
        return switch (fieldType) {
            case FIELD_TYPE_TEXT, FIELD_TYPE_PHONE -> fieldValue.getValueText() != null
                    ? fieldValue.getValueText() : EMPTY_STRING;
            case FIELD_TYPE_NUMBER -> fieldValue.getValueNumber() != null
                    ? fieldValue.getValueNumber().toString() : EMPTY_STRING;
            case FIELD_TYPE_DATE -> fieldValue.getValueDate() != null
                    ? fieldValue.getValueDate().toString() : EMPTY_STRING;
            case FIELD_TYPE_BOOLEAN -> formatBooleanValue(fieldValue.getValueBoolean());
            case FIELD_TYPE_LIST -> fieldValue.getValueListValue() != null
                    ? fieldValue.getValueListValue() : EMPTY_STRING;
            default -> EMPTY_STRING;
        };
    }

    private String formatBooleanValue(Boolean value) {
        if (value == null) {
            return EMPTY_STRING;
        }
        return value ? BOOLEAN_TRUE_UA : BOOLEAN_FALSE_UA;
    }

    private Long parseFieldId(@NonNull String field) {
        try {
            return Long.parseLong(field.substring(FIELD_PREFIX_LENGTH));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private <T extends IdNameDTO> String getNameFromDTOList(@NonNull List<T> dtoList, Long id) {
        if (id == null) {
            return EMPTY_STRING;
        }
        return dtoList.stream()
                .filter(dto -> dto.getId() != null && dto.getId().equals(id))
                .findFirst()
                .map(IdNameDTO::getName)
                .orElse(EMPTY_STRING);
    }
}
