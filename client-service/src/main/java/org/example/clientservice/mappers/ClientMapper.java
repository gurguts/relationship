package org.example.clientservice.mappers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.client.*;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.clientservice.models.dto.fields.SourceDTO;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.IClientTypeService;
import org.example.clientservice.services.impl.IClientTypeFieldService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientMapper {
    
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    private static final int DEFAULT_DISPLAY_ORDER = 0;
    
    private final IClientTypeService clientTypeService;
    private final IClientTypeFieldService clientTypeFieldService;
    private final org.example.clientservice.mappers.clienttype.ClientFieldValueMapper fieldValueMapper;


    public ClientDTO clientToClientDTO(@NonNull Client client) {
        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setId(client.getId());
        clientDTO.setCompany(client.getCompany());
        clientDTO.setIsActive(client.getIsActive());
        clientDTO.setCreatedAt(processTime(client.getCreatedAt()));
        clientDTO.setUpdatedAt(processTime(client.getUpdatedAt()));
        if (client.getSourceId() != null) {
            clientDTO.setSourceId(String.valueOf(client.getSourceId()));
        }
        
        if (client.getFieldValues() != null && !client.getFieldValues().isEmpty()) {
            List<ClientFieldValueDTO> fieldValueDTOs = client.getFieldValues().stream()
                    .map(fieldValueMapper::toDTO)
                    .collect(Collectors.toList());
            clientDTO.setFieldValues(fieldValueDTOs);
        } else {
            clientDTO.setFieldValues(Collections.emptyList());
        }

        return clientDTO;
    }

    private String processTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(DATE_TIME_FORMATTER);
    }

    public Client clientCreateDTOToClient(@NonNull ClientCreateDTO clientCreateDTO) {
        Client client = new Client();
        mapClientTypeFromCreateDTO(clientCreateDTO, client);
        client.setCompany(clientCreateDTO.getCompany());
        client.setSourceId(clientCreateDTO.getSourceId());
        mapFieldValuesFromCreateDTO(clientCreateDTO, client);

        return client;
    }

    private void mapClientTypeFromCreateDTO(@NonNull ClientCreateDTO clientCreateDTO, @NonNull Client client) {
        ClientType clientType = clientTypeService.getClientTypeById(clientCreateDTO.getClientTypeId());
        client.setClientType(clientType);
    }

    private void mapFieldValuesFromCreateDTO(@NonNull ClientCreateDTO clientCreateDTO, @NonNull Client client) {
        if (clientCreateDTO.getFieldValues() != null && !clientCreateDTO.getFieldValues().isEmpty()) {
            List<ClientFieldValue> fieldValues = mapFieldValuesFromDTOs(
                    clientCreateDTO.getFieldValues(), client);
            client.setFieldValues(fieldValues);
        }
    }

    public Client clientUpdateDTOtoClient(@NonNull ClientUpdateDTO clientUpdateDTO) {
        Client client = new Client();
        client.setCompany(clientUpdateDTO.getCompany());
        if (clientUpdateDTO.getSourceId() != null) {
            client.setSourceId(clientUpdateDTO.getSourceId());
        }
        mapFieldValuesFromUpdateDTO(clientUpdateDTO, client);

        return client;
    }

    private void mapFieldValuesFromUpdateDTO(@NonNull ClientUpdateDTO clientUpdateDTO, @NonNull Client client) {
        if (clientUpdateDTO.getFieldValues() != null && !clientUpdateDTO.getFieldValues().isEmpty()) {
            List<ClientFieldValue> fieldValues = mapFieldValuesFromDTOs(
                    clientUpdateDTO.getFieldValues(), client);
            client.setFieldValues(fieldValues);
        } else {
            client.setFieldValues(Collections.emptyList());
        }
    }
    
    private List<ClientFieldValue> mapFieldValuesFromDTOs(@NonNull List<? extends ClientFieldValueCreateDTO> dtos, 
                                                          @NonNull Client client) {
        Set<String> seenKeys = new LinkedHashSet<>();
        return dtos.stream()
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
                    fieldValue.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : DEFAULT_DISPLAY_ORDER);
                    return fieldValue;
                })
                .filter(fieldValue -> {
                    String key = buildFieldValueKey(fieldValue);
                    if (seenKeys.contains(key)) {
                        return false;
                    }
                    seenKeys.add(key);
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private String buildFieldValueKey(@NonNull ClientFieldValue fieldValue) {
        Long fieldId = fieldValue.getField().getId();
        if (fieldValue.getValueList() != null) {
            Long valueListId = fieldValue.getValueList().getId();
            return String.format("field:%d:list:%d", fieldId, valueListId);
        } else if (fieldValue.getValueText() != null) {
            return String.format("field:%d:text:%s", fieldId, fieldValue.getValueText());
        } else if (fieldValue.getValueNumber() != null) {
            return String.format("field:%d:number:%s", fieldId, fieldValue.getValueNumber());
        } else if (fieldValue.getValueDate() != null) {
            return String.format("field:%d:date:%s", fieldId, fieldValue.getValueDate());
        } else if (fieldValue.getValueBoolean() != null) {
            return String.format("field:%d:boolean:%s", fieldId, fieldValue.getValueBoolean());
        }
        return String.format("field:%d:empty", fieldId);
    }

    public ClientListDTO clientToClientListDTO(@NonNull Client client, @NonNull ExternalClientDataCache cache) {
        ClientListDTO clientDTO = new ClientListDTO();
        clientDTO.setId(client.getId());
        clientDTO.setCompany(client.getCompany());
        clientDTO.setIsActive(client.getIsActive());
        clientDTO.setCreatedAt(processTime(client.getCreatedAt()));
        clientDTO.setUpdatedAt(processTime(client.getUpdatedAt()));

        if (client.getSourceId() != null) {
            Source source = cache.sourceMap().get(client.getSourceId());
            if (source != null) {
                SourceDTO sourceDTO = new SourceDTO();
                sourceDTO.setId(source.getId());
                sourceDTO.setName(source.getName());
                sourceDTO.setUserId(source.getUserId());
                clientDTO.setSource(sourceDTO);
            }
        }

        return clientDTO;
    }
}
