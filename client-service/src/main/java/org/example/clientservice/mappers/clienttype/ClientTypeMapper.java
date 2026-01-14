package org.example.clientservice.mappers.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class ClientTypeMapper {

    public ClientTypeDTO clientTypeToDTO(@NonNull ClientType clientType) {
        ClientTypeDTO dto = new ClientTypeDTO();
        dto.setId(clientType.getId());
        dto.setName(clientType.getName());
        dto.setNameFieldLabel(clientType.getNameFieldLabel());
        dto.setIsActive(clientType.getIsActive());
        dto.setCreatedAt(clientType.getCreatedAt());
        dto.setUpdatedAt(clientType.getUpdatedAt());
        
        if (clientType.getFields() != null && !clientType.getFields().isEmpty()) {
            dto.setFields(clientType.getFields().stream()
                    .map(ClientTypeFieldMapper::toDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setFields(Collections.emptyList());
        }

        return dto;
    }

    public ClientType createDTOToClientType(@NonNull ClientTypeCreateDTO dto) {
        ClientType clientType = new ClientType();
        clientType.setName(dto.getName());
        clientType.setNameFieldLabel(dto.getNameFieldLabel());
        return clientType;
    }

    public void updateClientTypeFromDTO(@NonNull ClientType clientType, @NonNull ClientTypeUpdateDTO dto) {
        if (dto.getName() != null) {
            clientType.setName(dto.getName());
        }
        if (dto.getNameFieldLabel() != null) {
            clientType.setNameFieldLabel(dto.getNameFieldLabel());
        }
        if (dto.getIsActive() != null) {
            clientType.setIsActive(dto.getIsActive());
        }
    }
}

