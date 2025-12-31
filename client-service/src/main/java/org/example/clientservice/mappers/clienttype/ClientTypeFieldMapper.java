package org.example.clientservice.mappers.clienttype;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.example.clientservice.models.clienttype.FieldType;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.repositories.clienttype.ClientTypeFieldListValueRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientTypeFieldMapper {
    
    private static final boolean DEFAULT_IS_REQUIRED = false;
    private static final boolean DEFAULT_IS_SEARCHABLE = true;
    private static final boolean DEFAULT_IS_FILTERABLE = false;
    private static final boolean DEFAULT_IS_VISIBLE_IN_TABLE = true;
    private static final boolean DEFAULT_IS_VISIBLE_IN_CREATE = true;
    private static final int DEFAULT_DISPLAY_ORDER = 0;
    private static final boolean DEFAULT_ALLOW_MULTIPLE = false;
    
    private final ClientTypeFieldListValueRepository listValueRepository;

    public static ClientTypeFieldDTO toDTO(@NonNull ClientTypeField field) {
        ClientTypeFieldDTO dto = new ClientTypeFieldDTO();
        dto.setId(field.getId());
        dto.setFieldName(field.getFieldName());
        dto.setFieldLabel(field.getFieldLabel());
        
        if (field.getFieldType() != null) {
            dto.setFieldType(field.getFieldType().name());
        }
        
        dto.setIsRequired(field.getIsRequired());
        dto.setIsSearchable(field.getIsSearchable());
        dto.setIsFilterable(field.getIsFilterable());
        dto.setIsVisibleInTable(field.getIsVisibleInTable());
        dto.setIsVisibleInCreate(field.getIsVisibleInCreate());
        dto.setDisplayOrder(field.getDisplayOrder());
        dto.setColumnWidth(field.getColumnWidth());
        dto.setValidationPattern(field.getValidationPattern());
        dto.setAllowMultiple(field.getAllowMultiple());

        if (field.getListValues() != null) {
            dto.setListValues(field.getListValues().stream()
                    .map(ClientTypeFieldListValueMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public ClientTypeField createDTOToField(@NonNull ClientTypeFieldCreateDTO dto, 
                                            @NonNull org.example.clientservice.models.clienttype.ClientType clientType) {
        ClientTypeField field = new ClientTypeField();
        field.setClientType(clientType);
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(FieldType.valueOf(dto.getFieldType()));
        field.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : DEFAULT_IS_REQUIRED);
        field.setIsSearchable(dto.getIsSearchable() != null ? dto.getIsSearchable() : DEFAULT_IS_SEARCHABLE);
        field.setIsFilterable(dto.getIsFilterable() != null ? dto.getIsFilterable() : DEFAULT_IS_FILTERABLE);
        field.setIsVisibleInTable(dto.getIsVisibleInTable() != null ? dto.getIsVisibleInTable() : DEFAULT_IS_VISIBLE_IN_TABLE);
        field.setIsVisibleInCreate(dto.getIsVisibleInCreate() != null ? dto.getIsVisibleInCreate() : DEFAULT_IS_VISIBLE_IN_CREATE);
        field.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : DEFAULT_DISPLAY_ORDER);
        field.setColumnWidth(dto.getColumnWidth());
        field.setValidationPattern(dto.getValidationPattern());
        field.setAllowMultiple(dto.getAllowMultiple() != null ? dto.getAllowMultiple() : DEFAULT_ALLOW_MULTIPLE);

        if (dto.getListValues() != null && !dto.getListValues().isEmpty()) {
            field.setListValues(processListValues(dto.getListValues(), field));
        }

        return field;
    }
    
    private List<ClientTypeFieldListValue> processListValues(@NonNull List<String> values, @NonNull ClientTypeField field) {
        List<ClientTypeFieldListValue> listValues = new ArrayList<>();
        List<String> seenValues = new ArrayList<>();
        int order = 0;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                String trimmedValue = value.trim();
                if (!seenValues.contains(trimmedValue)) {
                    ClientTypeFieldListValue listValue = new ClientTypeFieldListValue();
                    listValue.setField(field);
                    listValue.setValue(trimmedValue);
                    listValue.setDisplayOrder(order++);
                    listValues.add(listValue);
                    seenValues.add(trimmedValue);
                }
            }
        }
        return listValues;
    }

    public void updateFieldFromDTO(@NonNull ClientTypeField field, @NonNull ClientTypeFieldUpdateDTO dto) {
        if (dto.getFieldLabel() != null) {
            field.setFieldLabel(dto.getFieldLabel());
        }
        if (dto.getIsRequired() != null) {
            field.setIsRequired(dto.getIsRequired());
        }
        if (dto.getIsSearchable() != null) {
            field.setIsSearchable(dto.getIsSearchable());
        }
        if (dto.getIsFilterable() != null) {
            field.setIsFilterable(dto.getIsFilterable());
        }
        if (dto.getIsVisibleInTable() != null) {
            field.setIsVisibleInTable(dto.getIsVisibleInTable());
        }
        if (dto.getIsVisibleInCreate() != null) {
            field.setIsVisibleInCreate(dto.getIsVisibleInCreate());
        }
        if (dto.getDisplayOrder() != null) {
            field.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getColumnWidth() != null) {
            field.setColumnWidth(dto.getColumnWidth());
        }
        if (dto.getValidationPattern() != null) {
            field.setValidationPattern(dto.getValidationPattern());
        }
        if (dto.getAllowMultiple() != null) {
            field.setAllowMultiple(dto.getAllowMultiple());
        }

        if (dto.getListValues() != null) {
            updateListValues(field, dto.getListValues());
        }
    }
    
    private void updateListValues(@NonNull ClientTypeField field, @NonNull List<String> newValuesList) {
        List<String> newValues = newValuesList.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
        
        if (field.getListValues() == null) {
            field.setListValues(new ArrayList<>());
        }
        
        Map<String, ClientTypeFieldListValue> existingValuesMap = field.getListValues().stream()
                .collect(Collectors.toMap(ClientTypeFieldListValue::getValue, lv -> lv, (lv1, _) -> lv1));
        
        List<ClientTypeFieldListValue> valuesToRemove = new ArrayList<>();
        for (ClientTypeFieldListValue existingValue : field.getListValues()) {
            if (!newValues.contains(existingValue.getValue())) {
                valuesToRemove.add(existingValue);
            }
        }
        field.getListValues().removeAll(valuesToRemove);
        
        int order = 0;
        for (String newValue : newValues) {
            ClientTypeFieldListValue listValue = existingValuesMap.get(newValue);
            if (listValue == null) {
                listValue = listValueRepository.findByFieldIdAndValue(field.getId(), newValue);
                if (listValue == null) {
                    listValue = new ClientTypeFieldListValue();
                    listValue.setField(field);
                    listValue.setValue(newValue);
                    field.getListValues().add(listValue);
                } else {
                    field.getListValues().add(listValue);
                }
            }
            listValue.setDisplayOrder(order++);
        }
    }
}

