package org.example.clientservice.mappers;

import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.client.*;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.clientservice.models.dto.fields.SourceDTO;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.clienttype.ClientTypeService;
import org.example.clientservice.services.clienttype.ClientTypeFieldService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientMapper {
    private final ClientTypeService clientTypeService;
    private final ClientTypeFieldService clientTypeFieldService;
    private final org.example.clientservice.mappers.clienttype.ClientFieldValueMapper fieldValueMapper;


    public ClientDTO clientToClientDTO(Client client) {
        if (client == null) {
            return null;
        }

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setId(client.getId());
        clientDTO.setCompany(client.getCompany());
        clientDTO.setIsActive(client.getIsActive());
        clientDTO.setCreatedAt(processTime(client.getCreatedAt()));
        clientDTO.setUpdatedAt(processTime(client.getUpdatedAt()));
        if (client.getSource() != null) {
            clientDTO.setSourceId(String.valueOf(client.getSource()));
        }
        
        if (client.getFieldValues() != null && !client.getFieldValues().isEmpty()) {
            List<ClientFieldValueDTO> fieldValueDTOs = client.getFieldValues().stream()
                    .map(fieldValueMapper::toDTO)
                    .collect(Collectors.toList());
            clientDTO.setFieldValues(fieldValueDTOs);
        } else {
            clientDTO.setFieldValues(new ArrayList<>());
        }

        return clientDTO;
    }

    private String processTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }

    public Client clientCreateDTOToClient(ClientCreateDTO clientCreateDTO) {
        Client client = new Client();
        mapClientTypeFromCreateDTO(clientCreateDTO, client);
        client.setCompany(clientCreateDTO.getCompany());
        client.setSource(clientCreateDTO.getSourceId());
        mapFieldValuesFromCreateDTO(clientCreateDTO, client);

        return client;
    }

    private void mapClientTypeFromCreateDTO(ClientCreateDTO clientCreateDTO, Client client) {
        if (clientCreateDTO.getClientTypeId() != null) {
            ClientType clientType = clientTypeService.getClientTypeById(clientCreateDTO.getClientTypeId());
            client.setClientType(clientType);
        }
    }

    private void mapFieldValuesFromCreateDTO(ClientCreateDTO clientCreateDTO, Client client) {
        if (clientCreateDTO.getFieldValues() != null && !clientCreateDTO.getFieldValues().isEmpty()) {
            List<ClientFieldValue> fieldValues = clientCreateDTO.getFieldValues().stream()
                    .map(dto -> {
                        ClientFieldValue fieldValue = new ClientFieldValue();
                        fieldValue.setClient(client);
                        fieldValue.setField(clientTypeFieldService.getFieldById(dto.getFieldId()));
                        fieldValue.setValueText(dto.getValueText());
                        fieldValue.setValueNumber(dto.getValueNumber());
                        fieldValue.setValueDate(dto.getValueDate());
                        fieldValue.setValueBoolean(dto.getValueBoolean());
                        if (dto.getValueListId() != null) {
                            fieldValue.setValueList(clientTypeService.getListValueById(dto.getValueListId()));
                        }
                        fieldValue.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
                        return fieldValue;
                    })
                    .collect(Collectors.toList());
            client.setFieldValues(fieldValues);
        }
    }

    public Client clientUpdateDTOtoClient(ClientUpdateDTO clientUpdateDTO) {
        Client client = new Client();
        client.setCompany(clientUpdateDTO.getCompany());
        if (clientUpdateDTO.getSourceId() != null) {
            client.setSource(clientUpdateDTO.getSourceId());
        }
        mapFieldValuesFromUpdateDTO(clientUpdateDTO, client);

        return client;
    }

    private void mapFieldValuesFromUpdateDTO(ClientUpdateDTO clientUpdateDTO, Client client) {
        if (clientUpdateDTO.getFieldValues() != null && !clientUpdateDTO.getFieldValues().isEmpty()) {
            List<ClientFieldValue> fieldValues = clientUpdateDTO.getFieldValues().stream()
                    .map(dto -> {
                        ClientFieldValue fieldValue = new ClientFieldValue();
                        fieldValue.setClient(client);
                        fieldValue.setField(clientTypeFieldService.getFieldById(dto.getFieldId()));
                        fieldValue.setValueText(dto.getValueText());
                        fieldValue.setValueNumber(dto.getValueNumber());
                        fieldValue.setValueDate(dto.getValueDate());
                        fieldValue.setValueBoolean(dto.getValueBoolean());
                        if (dto.getValueListId() != null) {
                            fieldValue.setValueList(clientTypeService.getListValueById(dto.getValueListId()));
                        }
                        fieldValue.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
                        return fieldValue;
                    })
                    .collect(Collectors.toList());
            client.setFieldValues(fieldValues);
        } else {
            client.setFieldValues(new ArrayList<>());
        }
    }

    public ClientListDTO clientToClientListDTO(Client client, ExternalClientDataCache cache) {
        if (client == null) {
            return null;
        }

        ClientListDTO clientDTO = new ClientListDTO();
        clientDTO.setId(client.getId());
        clientDTO.setCompany(client.getCompany());
        clientDTO.setIsActive(client.getIsActive());
        clientDTO.setCreatedAt(processTime(client.getCreatedAt()));
        clientDTO.setUpdatedAt(processTime(client.getUpdatedAt()));

        Source source = cache.getSourceMap().get(client.getSource());
        if (source != null) {
            SourceDTO sourceDTO = new SourceDTO();
            sourceDTO.setId(source.getId());
            sourceDTO.setName(source.getName());
            sourceDTO.setUserId(source.getUserId());
            clientDTO.setSource(sourceDTO);
        }

        return clientDTO;
    }
}
