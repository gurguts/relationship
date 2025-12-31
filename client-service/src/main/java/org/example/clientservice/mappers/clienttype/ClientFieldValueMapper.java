package org.example.clientservice.mappers.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.springframework.stereotype.Component;

@Component
public class ClientFieldValueMapper {

    public ClientFieldValueDTO toDTO(@NonNull ClientFieldValue fieldValue) {
        ClientFieldValueDTO dto = new ClientFieldValueDTO();
        dto.setId(fieldValue.getId());
        
        if (fieldValue.getField() == null) {
            return dto;
        }
        
        dto.setFieldId(fieldValue.getField().getId());
        dto.setFieldName(fieldValue.getField().getFieldName());
        dto.setFieldLabel(fieldValue.getField().getFieldLabel());
        
        if (fieldValue.getField().getFieldType() != null) {
            dto.setFieldType(fieldValue.getField().getFieldType().name());
        }
        
        dto.setValueText(fieldValue.getValueText());
        dto.setValueNumber(fieldValue.getValueNumber());
        dto.setValueDate(fieldValue.getValueDate());
        dto.setValueBoolean(fieldValue.getValueBoolean());
        dto.setValueListId(fieldValue.getValueList() != null ? fieldValue.getValueList().getId() : null);
        dto.setValueListValue(fieldValue.getValueList() != null ? fieldValue.getValueList().getValue() : null);
        dto.setDisplayOrder(fieldValue.getDisplayOrder());
        return dto;
    }
}

