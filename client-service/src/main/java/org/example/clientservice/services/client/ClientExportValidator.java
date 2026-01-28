package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.services.impl.IClientTypeFieldService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientExportValidator {

    private static final Set<String> VALID_STATIC_FIELDS = Set.of(
            "id", "company", "createdAt", "updatedAt", "source");
    private static final int MAX_QUERY_LENGTH = 255;
    private static final String FIELD_PREFIX = "field_";
    private static final int FIELD_PREFIX_LENGTH = 6;

    private final IClientTypeFieldService clientTypeFieldService;

    public void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ClientException("INVALID_QUERY", 
                    String.format("Search query cannot exceed %d characters", MAX_QUERY_LENGTH));
        }
    }

    public void validateSelectedFields(@NonNull List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        
        List<Long> fieldIds = extractFieldIds(selectedFields);
        Map<Long, ClientTypeField> fieldMap = loadFieldMap(fieldIds);
        
        validateFieldNames(selectedFields, fieldMap);
    }

    private List<Long> extractFieldIds(@NonNull List<String> selectedFields) {
        return selectedFields.stream()
                .filter(field -> field.startsWith(FIELD_PREFIX))
                .map(this::parseFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long parseFieldId(@NonNull String field) {
        try {
            return Long.parseLong(field.substring(FIELD_PREFIX_LENGTH));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private Map<Long, ClientTypeField> loadFieldMap(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        FieldIdsRequest request = new FieldIdsRequest(fieldIds);
        return clientTypeFieldService.getFieldsByIds(request).stream()
                .collect(Collectors.toMap(ClientTypeField::getId, field -> field));
    }

    private void validateFieldNames(@NonNull List<String> selectedFields, @NonNull Map<Long, ClientTypeField> fieldMap) {
        for (String field : selectedFields) {
            if (!VALID_STATIC_FIELDS.contains(field) && !field.startsWith(FIELD_PREFIX)) {
                throw new ClientException("INVALID_FIELDS", 
                        String.format("Invalid field specified for export: %s", field));
            }
            if (field.startsWith(FIELD_PREFIX)) {
                validateDynamicField(field, fieldMap);
            }
        }
    }

    private void validateDynamicField(@NonNull String field, @NonNull Map<Long, ClientTypeField> fieldMap) {
        Long fieldId = parseFieldId(field);
        if (fieldId == null) {
            throw new ClientException("INVALID_FIELDS", 
                    String.format("Invalid field ID format: %s", field));
        }
        if (!fieldMap.containsKey(fieldId)) {
            throw new ClientException("INVALID_FIELDS", 
                    String.format("Dynamic field not found: %s", field));
        }
    }

    public Long parseFieldIdFromString(@NonNull String field) {
        return parseFieldId(field);
    }
}
