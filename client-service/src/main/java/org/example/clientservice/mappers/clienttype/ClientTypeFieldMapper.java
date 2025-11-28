package org.example.clientservice.mappers.clienttype;

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
    private final ClientTypeFieldListValueRepository listValueRepository;

    public static ClientTypeFieldDTO toDTO(ClientTypeField field) {
        if (field == null) {
            return null;
        }

        ClientTypeFieldDTO dto = new ClientTypeFieldDTO();
        dto.setId(field.getId());
        dto.setFieldName(field.getFieldName());
        dto.setFieldLabel(field.getFieldLabel());
        dto.setFieldType(field.getFieldType().name());
        dto.setIsRequired(field.getIsRequired());
        dto.setIsSearchable(field.getIsSearchable());
        dto.setIsFilterable(field.getIsFilterable());
        dto.setIsVisibleInTable(field.getIsVisibleInTable());
        dto.setIsVisibleInCreate(field.getIsVisibleInCreate());
        dto.setDisplayOrder(field.getDisplayOrder());
        dto.setValidationPattern(field.getValidationPattern());
        dto.setAllowMultiple(field.getAllowMultiple());

        if (field.getListValues() != null) {
            dto.setListValues(field.getListValues().stream()
                    .map(ClientTypeFieldListValueMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public ClientTypeField createDTOToField(ClientTypeFieldCreateDTO dto, org.example.clientservice.models.clienttype.ClientType clientType) {
        if (dto == null) {
            return null;
        }

        ClientTypeField field = new ClientTypeField();
        field.setClientType(clientType);
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(FieldType.valueOf(dto.getFieldType()));
        field.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false);
        field.setIsSearchable(dto.getIsSearchable() != null ? dto.getIsSearchable() : true);
        field.setIsFilterable(dto.getIsFilterable() != null ? dto.getIsFilterable() : false);
        field.setIsVisibleInTable(dto.getIsVisibleInTable() != null ? dto.getIsVisibleInTable() : true);
        field.setIsVisibleInCreate(dto.getIsVisibleInCreate() != null ? dto.getIsVisibleInCreate() : true);
        field.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        field.setValidationPattern(dto.getValidationPattern());
        field.setAllowMultiple(dto.getAllowMultiple() != null ? dto.getAllowMultiple() : false);

        if (dto.getListValues() != null && !dto.getListValues().isEmpty()) {
            List<ClientTypeFieldListValue> listValues = new ArrayList<>();
            List<String> seenValues = new ArrayList<>();
            int order = 0;
            for (String value : dto.getListValues()) {
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
            field.setListValues(listValues);
        }

        return field;
    }

    public void updateFieldFromDTO(ClientTypeField field, ClientTypeFieldUpdateDTO dto) {
        if (dto == null || field == null) {
            return;
        }

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
        if (dto.getValidationPattern() != null) {
            field.setValidationPattern(dto.getValidationPattern());
        }
        if (dto.getAllowMultiple() != null) {
            field.setAllowMultiple(dto.getAllowMultiple());
        }

        if (dto.getListValues() != null) {
            List<String> newValues = dto.getListValues().stream()
                    .filter(v -> v != null && !v.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            
            Map<String, ClientTypeFieldListValue> existingValuesMap = field.getListValues().stream()
                    .collect(Collectors.toMap(ClientTypeFieldListValue::getValue, lv -> lv, (lv1, lv2) -> lv1));
            
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
}

