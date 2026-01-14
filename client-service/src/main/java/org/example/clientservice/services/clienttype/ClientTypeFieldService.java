package org.example.clientservice.services.clienttype;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.mappers.clienttype.ClientTypeFieldMapper;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldsAllDTO;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;
import org.example.clientservice.repositories.clienttype.ClientTypeFieldRepository;
import org.example.clientservice.services.impl.IClientTypeFieldService;
import org.example.clientservice.services.impl.IClientTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTypeFieldService implements IClientTypeFieldService {
    private static final int DEFAULT_DISPLAY_ORDER = 999;
    
    private final ClientTypeFieldRepository fieldRepository;
    private final ClientTypeFieldMapper fieldMapper;
    private final IClientTypeService clientTypeService;

    @Override
    @Transactional
    @NonNull
    public ClientTypeField createField(@NonNull Long clientTypeId, @NonNull ClientTypeFieldCreateDTO dto) {
        log.info("Creating field {} for client type {}", dto.getFieldName(), clientTypeId);
        
        try {
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);

            if (fieldRepository.findByClientTypeIdAndFieldName(clientTypeId, dto.getFieldName()).isPresent()) {
                throw new ClientException("FIELD_ALREADY_EXISTS", 
                    String.format("Field with name %s already exists in this client type", dto.getFieldName()));
            }

            ClientTypeField field = fieldMapper.createDTOToField(dto, clientType);
            return fieldRepository.save(field);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating field {} for client type {}: {}", dto.getFieldName(), clientTypeId, e.getMessage(), e);
            throw new ClientException("FIELD_CREATION_ERROR",
                    String.format("Failed to create field: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    @NonNull
    public ClientTypeField updateField(@NonNull Long fieldId, @NonNull ClientTypeFieldUpdateDTO dto) {
        log.info("Updating field with ID: {}", fieldId);
        
        try {
            ClientTypeField field = getFieldById(fieldId);
            fieldMapper.updateFieldFromDTO(field, dto);
            return fieldRepository.save(field);
        } catch (ClientNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating field with ID {}: {}", fieldId, e.getMessage(), e);
            throw new ClientException("FIELD_UPDATE_ERROR",
                    String.format("Failed to update field: %s", e.getMessage()), e);
        }
    }

    @Override
    @NonNull
    public ClientTypeField getFieldById(@NonNull Long fieldId) {
        return fieldRepository.findByIdWithListValues(fieldId)
                .orElseThrow(() -> new ClientNotFoundException("Field not found with id: " + fieldId));
    }

    @Override
    @NonNull
    public List<ClientTypeField> getFieldsByClientTypeId(@NonNull Long clientTypeId) {
        return fieldRepository.findByClientTypeIdOrderByDisplayOrderAscWithListValues(clientTypeId);
    }

    @Override
    @NonNull
    public List<ClientTypeField> getVisibleFieldsByClientTypeId(@NonNull Long clientTypeId) {
        return fieldRepository.findVisibleFieldsByClientTypeId(clientTypeId);
    }

    @Override
    @NonNull
    public List<ClientTypeField> getSearchableFieldsByClientTypeId(@NonNull Long clientTypeId) {
        return fieldRepository.findSearchableFieldsByClientTypeId(clientTypeId);
    }

    @Override
    @NonNull
    public List<ClientTypeField> getFilterableFieldsByClientTypeId(@NonNull Long clientTypeId) {
        return fieldRepository.findFilterableFieldsByClientTypeId(clientTypeId);
    }

    @Override
    @NonNull
    public List<ClientTypeField> getVisibleInCreateFieldsByClientTypeId(@NonNull Long clientTypeId) {
        return fieldRepository.findVisibleInCreateFieldsByClientTypeId(clientTypeId);
    }

    @Override
    @NonNull
    public List<ClientTypeField> getFieldsByIds(@NonNull FieldIdsRequest request) {
        validateFieldIdsRequest(request);
        return fieldRepository.findByIdsWithListValues(request.fieldIds());
    }

    @Override
    @Transactional
    public void deleteField(@NonNull Long fieldId) {
        log.info("Deleting field with ID: {}", fieldId);
        
        try {
            ClientTypeField field = getFieldById(fieldId);
            fieldRepository.delete(field);
        } catch (ClientNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting field with ID {}: {}", fieldId, e.getMessage(), e);
            throw new ClientException("FIELD_DELETION_ERROR",
                    String.format("Failed to delete field: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    public void reorderFields(@NonNull Long clientTypeId, @NonNull FieldReorderDTO dto) {
        log.info("Reordering fields for client type {}", clientTypeId);
        
        try {
            validateReorderRequest(clientTypeId, dto);
            
            List<ClientTypeField> fields = fieldRepository.findByClientTypeIdOrderByDisplayOrderAsc(clientTypeId);
            Map<Long, ClientTypeField> fieldMap = buildFieldMap(fields);
            
            updateFieldDisplayOrders(dto, fieldMap);
            
            fieldRepository.saveAll(fields);
        } catch (ClientException | ClientNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error reordering fields for client type {}: {}", clientTypeId, e.getMessage(), e);
            throw new ClientException("FIELD_REORDER_ERROR",
                    String.format("Failed to reorder fields: %s", e.getMessage()), e);
        }
    }

    @Override
    @NonNull
    public List<ClientTypeFieldDTO> getVisibleFieldsWithStatic(@NonNull Long clientTypeId) {
        List<ClientTypeField> fields = getVisibleFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = mapFieldsToDTOs(fields);

        addStaticFieldsToResponse(response, clientTypeId);

        return response;
    }

    @Override
    @NonNull
    public ClientTypeFieldsAllDTO getAllFieldsByClientTypeId(@NonNull Long clientTypeId) {
        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
        
        ClientTypeFieldsAllDTO dto = new ClientTypeFieldsAllDTO();
        dto.setAll(mapFieldsToDTOs(getFieldsByClientTypeId(clientTypeId)));
        dto.setVisible(mapFieldsToDTOs(getVisibleFieldsByClientTypeId(clientTypeId)));
        dto.setSearchable(mapFieldsToDTOs(getSearchableFieldsByClientTypeId(clientTypeId)));
        dto.setFilterable(mapFieldsToDTOs(getFilterableFieldsByClientTypeId(clientTypeId)));
        dto.setVisibleInCreate(mapFieldsToDTOs(getVisibleInCreateFieldsByClientTypeId(clientTypeId)));
        
        addStaticFieldsToVisible(dto, clientType);

        return dto;
    }
    
    private void validateFieldIdsRequest(@NonNull FieldIdsRequest request) {
        if (request.fieldIds().isEmpty()) {
            throw new ClientException("INVALID_FIELD_IDS", "List of field IDs cannot be empty");
        }
    }
    
    private void validateReorderRequest(@NonNull Long clientTypeId, @NonNull FieldReorderDTO dto) {
        if (dto.getFieldIds() == null || dto.getFieldIds().isEmpty()) {
            throw new ClientException("INVALID_FIELD_IDS", "List of field IDs for reordering cannot be empty");
        }
        
        if (dto.getFieldIds().contains(null)) {
            throw new ClientException("INVALID_FIELD_IDS", "List of field IDs cannot contain null values");
        }
        
        clientTypeService.getClientTypeById(clientTypeId);
    }
    
    private Map<Long, ClientTypeField> buildFieldMap(@NonNull List<ClientTypeField> fields) {
        return fields.stream()
                .filter(field -> field.getId() != null)
                .collect(Collectors.toMap(ClientTypeField::getId, field -> field));
    }
    
    private void updateFieldDisplayOrders(@NonNull FieldReorderDTO dto, @NonNull Map<Long, ClientTypeField> fieldMap) {
        if (fieldMap.size() != dto.getFieldIds().size()) {
            throw new ClientException("FIELD_COUNT_MISMATCH", 
                    String.format("Field count mismatch: expected %d, got %d", fieldMap.size(), dto.getFieldIds().size()));
        }

        for (int i = 0; i < dto.getFieldIds().size(); i++) {
            Long fieldId = dto.getFieldIds().get(i);
            ClientTypeField field = fieldMap.get(fieldId);
            
            if (field == null) {
                throw new ClientNotFoundException("Field not found with id: " + fieldId);
            }
            
            field.setDisplayOrder(i);
        }
    }
    
    private List<ClientTypeFieldDTO> mapFieldsToDTOs(@NonNull List<ClientTypeField> fields) {
        return fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    private void addStaticFieldsToResponse(@NonNull List<ClientTypeFieldDTO> response, @NonNull Long clientTypeId) {
        try {
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
            StaticFieldsConfig staticConfig = StaticFieldsHelper.parseStaticFieldsConfig(clientType);
            
            if (staticConfig != null) {
                List<ClientTypeFieldDTO> staticFields = StaticFieldsHelper.createStaticFieldDTOs(staticConfig);
                response.addAll(staticFields);
                sortFieldsByDisplayOrder(response);
            }
        } catch (Exception e) {
            log.warn("Failed to add static fields for client type {}: {}", clientTypeId, e.getMessage());
        }
    }
    
    private void addStaticFieldsToVisible(@NonNull ClientTypeFieldsAllDTO dto, @NonNull ClientType clientType) {
        try {
            StaticFieldsConfig staticConfig = StaticFieldsHelper.parseStaticFieldsConfig(clientType);
            
            if (staticConfig != null) {
                List<ClientTypeFieldDTO> staticFields = StaticFieldsHelper.createStaticFieldDTOs(staticConfig);
                dto.getVisible().addAll(staticFields);
                sortFieldsByDisplayOrder(dto.getVisible());
            }
        } catch (Exception e) {
            log.warn("Failed to add static fields to visible: {}", e.getMessage());
        }
    }
    
    private void sortFieldsByDisplayOrder(@NonNull List<ClientTypeFieldDTO> fields) {
        fields.sort(Comparator.comparingInt(field -> 
                field.getDisplayOrder() != null ? field.getDisplayOrder() : DEFAULT_DISPLAY_ORDER));
    }
}
