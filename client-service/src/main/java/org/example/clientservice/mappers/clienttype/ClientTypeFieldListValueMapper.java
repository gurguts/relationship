package org.example.clientservice.mappers.clienttype;

import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldListValueDTO;
import org.springframework.stereotype.Component;

@Component
public class ClientTypeFieldListValueMapper {

    public static ClientTypeFieldListValueDTO toDTO(ClientTypeFieldListValue listValue) {
        if (listValue == null) {
            return null;
        }

        ClientTypeFieldListValueDTO dto = new ClientTypeFieldListValueDTO();
        dto.setId(listValue.getId());
        dto.setValue(listValue.getValue());
        dto.setDisplayOrder(listValue.getDisplayOrder());
        return dto;
    }
}

