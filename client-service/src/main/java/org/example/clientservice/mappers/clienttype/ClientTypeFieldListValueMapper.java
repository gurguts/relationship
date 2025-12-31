package org.example.clientservice.mappers.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldListValueDTO;

public final class ClientTypeFieldListValueMapper {

    private ClientTypeFieldListValueMapper() {
    }

    public static ClientTypeFieldListValueDTO toDTO(@NonNull ClientTypeFieldListValue listValue) {
        ClientTypeFieldListValueDTO dto = new ClientTypeFieldListValueDTO();
        dto.setId(listValue.getId());
        dto.setValue(listValue.getValue());
        dto.setDisplayOrder(listValue.getDisplayOrder());
        return dto;
    }
}

