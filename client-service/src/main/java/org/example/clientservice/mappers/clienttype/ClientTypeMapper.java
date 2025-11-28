package org.example.clientservice.mappers.clienttype;

import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ClientTypeMapper {

    public ClientTypeDTO clientTypeToDTO(ClientType clientType) {
        if (clientType == null) {
            return null;
        }

        ClientTypeDTO dto = new ClientTypeDTO();
        dto.setId(clientType.getId());
        dto.setName(clientType.getName());
        dto.setNameFieldLabel(clientType.getNameFieldLabel());
        dto.setIsActive(clientType.getIsActive());
        dto.setCreatedAt(clientType.getCreatedAt());
        dto.setUpdatedAt(clientType.getUpdatedAt());
        
        if (clientType.getFields() != null) {
            dto.setFields(clientType.getFields().stream()
                    .map(ClientTypeFieldMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public ClientType createDTOToClientType(ClientTypeCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        ClientType clientType = new ClientType();
        clientType.setName(dto.getName());
        clientType.setNameFieldLabel(dto.getNameFieldLabel() != null ? dto.getNameFieldLabel() : "Компанія");
        return clientType;
    }

    public void updateClientTypeFromDTO(ClientType clientType, ClientTypeUpdateDTO dto) {
        if (dto == null || clientType == null) {
            return;
        }

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

