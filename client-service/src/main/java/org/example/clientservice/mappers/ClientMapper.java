package org.example.clientservice.mappers;

import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PhoneNumber;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.client.*;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueCreateDTO;
import org.example.clientservice.models.dto.fields.*;
import org.example.clientservice.models.field.*;
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


    public ClientDTO clientToClientDTO(Client client) {
        if (client == null) {
            return null;
        }

        ClientDTO clientDTO = new ClientDTO();
        mapBasicFields(client, clientDTO);
        mapPhoneNumbers(client, clientDTO);
        mapExternalData(client, clientDTO);

        return clientDTO;
    }

    private void mapBasicFields(Client client, ClientDTO clientDTO) {
        clientDTO.setId(client.getId());
        clientDTO.setCompany(client.getCompany());
        clientDTO.setPerson(client.getPerson());
        clientDTO.setLocation(client.getLocation());
        clientDTO.setPricePurchase(client.getPricePurchase());
        clientDTO.setPriceSale(client.getPriceSale());
        clientDTO.setIsActive(client.getIsActive());
        clientDTO.setCreatedAt(processTime(client.getCreatedAt()));
        clientDTO.setUpdatedAt(processTime(client.getUpdatedAt()));
        clientDTO.setVolumeMonth(client.getVolumeMonth());
        clientDTO.setComment(client.getComment());
        clientDTO.setUrgently(client.getUrgently());
        clientDTO.setEdrpou(client.getEdrpou());
        clientDTO.setEnterpriseName(client.getEnterpriseName());
        clientDTO.setVat(client.getVat());
    }

    private void mapPhoneNumbers(Client client, ClientDTO clientDTO) {
        List<String> phoneNumbers = client.getPhoneNumbers().stream()
                .map(PhoneNumber::getNumber)
                .collect(Collectors.toList());
        clientDTO.setPhoneNumbers(phoneNumbers);
    }

    private void mapExternalData(Client client, ClientDTO clientDTO) {
        clientDTO.setBusinessId(String.valueOf(client.getBusiness()));
        clientDTO.setRouteId(String.valueOf(client.getRoute()));
        clientDTO.setRegionId(String.valueOf(client.getRegion()));
        clientDTO.setStatusId(String.valueOf(client.getStatus()));
        clientDTO.setSourceId(String.valueOf(client.getSource()));
        clientDTO.setClientProductId(String.valueOf(client.getClientProduct()));
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
        mapBasicFieldsFromCreateDTO(clientCreateDTO, client);
        mapPhoneNumbersFromCreateDTO(clientCreateDTO, client);
        mapExternalDataFromCreateDTO(clientCreateDTO, client);
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

    private void mapBasicFieldsFromCreateDTO(ClientCreateDTO clientCreateDTO, Client client) {
        client.setCompany(clientCreateDTO.getCompany());
        client.setPerson(clientCreateDTO.getPerson());
        client.setLocation(clientCreateDTO.getLocation());
        client.setPricePurchase(clientCreateDTO.getPricePurchase());
        client.setPriceSale(clientCreateDTO.getPriceSale());
        client.setComment(clientCreateDTO.getComment());
        client.setVolumeMonth(clientCreateDTO.getVolumeMonth());
    }

    private void mapPhoneNumbersFromCreateDTO(ClientCreateDTO clientCreateDTO, Client client) {
        if (clientCreateDTO.getPhoneNumbers() != null && !clientCreateDTO.getPhoneNumbers().isEmpty()) {
            List<PhoneNumber> phoneNumbers = clientCreateDTO.getPhoneNumbers().stream()
                    .map(number -> {
                        PhoneNumber phoneNumber = new PhoneNumber();
                        phoneNumber.setNumber(number);
                        phoneNumber.setClient(client);
                        return phoneNumber;
                    })
                    .collect(Collectors.toList());
            client.setPhoneNumbers(phoneNumbers);
        } else {
            client.setPhoneNumbers(new ArrayList<>());
        }
    }

    private void mapExternalDataFromCreateDTO(ClientCreateDTO clientCreateDTO, Client client) {
        client.setRoute(clientCreateDTO.getRouteId());
        client.setRegion(clientCreateDTO.getRegionId());
        client.setStatus(clientCreateDTO.getStatusId());
        client.setSource(clientCreateDTO.getSourceId());
        client.setBusiness(clientCreateDTO.getBusinessId());
        client.setClientProduct(clientCreateDTO.getClientProductId());
    }

    public Client clientUpdateDTOtoClient(ClientUpdateDTO clientUpdateDTO) {
        Client client = new Client();
        mapBasicFieldsFromUpdateDTO(clientUpdateDTO, client);
        mapPhoneNumbersFromUpdateDTO(clientUpdateDTO, client);
        mapExternalDataFromUpdateDTO(clientUpdateDTO, client);
        mapAdditionalFieldsFromUpdateDTO(clientUpdateDTO, client);
        mapFieldValuesFromUpdateDTO(clientUpdateDTO, client);

        return client;
    }

    private void mapBasicFieldsFromUpdateDTO(ClientUpdateDTO clientUpdateDTO, Client client) {
        client.setCompany(clientUpdateDTO.getCompany());
        client.setPerson(clientUpdateDTO.getPerson());
        client.setLocation(clientUpdateDTO.getLocation());
        client.setPricePurchase(clientUpdateDTO.getPricePurchase());
        client.setPriceSale(clientUpdateDTO.getPriceSale());
        client.setComment(clientUpdateDTO.getComment());
        client.setVolumeMonth(clientUpdateDTO.getVolumeMonth());
    }

    private void mapPhoneNumbersFromUpdateDTO(ClientUpdateDTO clientUpdateDTO, Client client) {
        List<PhoneNumber> phoneNumbers = clientUpdateDTO.getPhoneNumbers().stream()
                .map(number -> {
                    PhoneNumber phoneNumber = new PhoneNumber();
                    phoneNumber.setNumber(number);
                    phoneNumber.setClient(client);
                    return phoneNumber;
                })
                .collect(Collectors.toList());
        client.setPhoneNumbers(phoneNumbers);
    }

    private void mapExternalDataFromUpdateDTO(ClientUpdateDTO clientUpdateDTO, Client client) {
        client.setRoute(clientUpdateDTO.getRouteId());
        client.setRegion(clientUpdateDTO.getRegionId());
        client.setStatus(clientUpdateDTO.getStatusId());
        client.setSource(clientUpdateDTO.getSourceId());
        client.setBusiness(clientUpdateDTO.getBusinessId());
        client.setClientProduct(clientUpdateDTO.getClientProductId());
    }

    private void mapAdditionalFieldsFromUpdateDTO(ClientUpdateDTO clientUpdateDTO, Client client) {
        client.setEdrpou(clientUpdateDTO.getEdrpou());
        client.setEnterpriseName(clientUpdateDTO.getEnterpriseName());
        client.setVat(clientUpdateDTO.getVat());
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
        mapBasicListFields(client, clientDTO);
        mapListPhoneNumbers(client, clientDTO);
        mapExternalListData(client, clientDTO, cache);

        return clientDTO;
    }

    private void mapBasicListFields(Client client, ClientListDTO clientDTO) {
        clientDTO.setId(client.getId());
        clientDTO.setCompany(client.getCompany());
        clientDTO.setPerson(client.getPerson());
        clientDTO.setLocation(client.getLocation());
        clientDTO.setPricePurchase(client.getPricePurchase());
        clientDTO.setPriceSale(client.getPriceSale());
        clientDTO.setIsActive(client.getIsActive());
        clientDTO.setCreatedAt(processTime(client.getCreatedAt()));
        clientDTO.setUpdatedAt(processTime(client.getUpdatedAt()));
        clientDTO.setVolumeMonth(client.getVolumeMonth());
        clientDTO.setComment(client.getComment());
        clientDTO.setUrgently(client.getUrgently());
        clientDTO.setEdrpou(client.getEdrpou());
        clientDTO.setEnterpriseName(client.getEnterpriseName());
        clientDTO.setVat(client.getVat());
    }

    private void mapListPhoneNumbers(Client client, ClientListDTO clientDTO) {
        List<String> phoneNumbers = client.getPhoneNumbers().stream()
                .map(PhoneNumber::getNumber)
                .collect(Collectors.toList());
        clientDTO.setPhoneNumbers(phoneNumbers);
    }

    private void mapExternalListData(Client client, ClientListDTO clientDTO, ExternalClientDataCache cache) {
        Business business = cache.getBusinessMap().get(client.getBusiness());
        if (business != null) {
            clientDTO.setBusiness(new BusinessDTO(business.getId(), business.getName()));
        }

        Route route = cache.getRouteMap().get(client.getRoute());
        if (route != null) {
            clientDTO.setRoute(new RouteDTO(route.getId(), route.getName()));
        }

        Region region = cache.getRegionMap().get(client.getRegion());
        if (region != null) {
            clientDTO.setRegion(new RegionDTO(region.getId(), region.getName()));
        }

        StatusClient status = cache.getStatusClientMap().get(client.getStatus());
        if (status != null) {
            clientDTO.setStatus(new StatusDTO(status.getId(), status.getName()));
        }

        Source source = cache.getSourceMap().get(client.getSource());
        if (source != null) {
            clientDTO.setSource(new SourceDTO(source.getId(), source.getName()));
        }

        ClientProduct clientProduct = cache.getClientProductMap().get(client.getClientProduct());
        if (clientProduct != null) {
            clientDTO.setClientProduct(new ClientProductDTO(clientProduct.getId(), clientProduct.getName()));
        }
    }
}
