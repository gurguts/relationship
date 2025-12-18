package org.example.clientservice.services.clienttype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.mappers.clienttype.ClientTypeFieldMapper;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;
import org.example.clientservice.repositories.clienttype.ClientTypeFieldRepository;
import org.example.clientservice.services.impl.IClientTypeFieldService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTypeFieldService implements IClientTypeFieldService {
    private final ClientTypeFieldRepository fieldRepository;
    private final ClientTypeFieldMapper fieldMapper;
    private final ClientTypeService clientTypeService;

    @Override
    @Transactional
    public ClientTypeField createField(Long clientTypeId, ClientTypeFieldCreateDTO dto) {
        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);

        if (fieldRepository.findByClientTypeIdAndFieldName(clientTypeId, dto.getFieldName()).isPresent()) {
            throw new ClientException("Field with name " + dto.getFieldName() + " already exists in this client type");
        }

        ClientTypeField field = fieldMapper.createDTOToField(dto, clientType);
        log.info("Creating field {} for client type {}", dto.getFieldName(), clientTypeId);
        return fieldRepository.save(field);
    }

    @Override
    @Transactional
    public ClientTypeField updateField(Long fieldId, ClientTypeFieldUpdateDTO dto) {
        ClientTypeField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ClientNotFoundException("Field not found with id: " + fieldId));
        field.getListValues().size();
        fieldMapper.updateFieldFromDTO(field, dto);
        log.info("Updating field with ID: {}", fieldId);
        return fieldRepository.save(field);
    }

    @Override
    public ClientTypeField getFieldById(Long fieldId) {
        return fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ClientNotFoundException("Field not found with id: " + fieldId));
    }

    @Override
    public List<ClientTypeField> getFieldsByClientTypeId(Long clientTypeId) {
        return fieldRepository.findByClientTypeIdOrderByDisplayOrderAsc(clientTypeId);
    }

    @Override
    public List<ClientTypeField> getVisibleFieldsByClientTypeId(Long clientTypeId) {
        return fieldRepository.findVisibleFieldsByClientTypeId(clientTypeId);
    }

    @Override
    public List<ClientTypeField> getSearchableFieldsByClientTypeId(Long clientTypeId) {
        return fieldRepository.findSearchableFieldsByClientTypeId(clientTypeId);
    }

    @Override
    public List<ClientTypeField> getFilterableFieldsByClientTypeId(Long clientTypeId) {
        return fieldRepository.findFilterableFieldsByClientTypeId(clientTypeId);
    }

    @Override
    public List<ClientTypeField> getVisibleInCreateFieldsByClientTypeId(Long clientTypeId) {
        return fieldRepository.findVisibleInCreateFieldsByClientTypeId(clientTypeId);
    }

    @Override
    public List<ClientTypeField> getFieldsByIds(List<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return fieldRepository.findAllById(fieldIds);
    }

    @Override
    @Transactional
    public void deleteField(Long fieldId) {
        ClientTypeField field = getFieldById(fieldId);
        log.info("Deleting field with ID: {}", fieldId);
        fieldRepository.delete(field);
    }

    @Override
    @Transactional
    public void reorderFields(Long clientTypeId, FieldReorderDTO dto) {
        clientTypeService.getClientTypeById(clientTypeId);
        
        List<ClientTypeField> fields = fieldRepository.findByClientTypeIdOrderByDisplayOrderAsc(clientTypeId);
        
        if (fields.size() != dto.getFieldIds().size()) {
            throw new ClientException("Field count mismatch");
        }

        for (int i = 0; i < dto.getFieldIds().size(); i++) {
            Long fieldId = dto.getFieldIds().get(i);
            ClientTypeField field = fields.stream()
                    .filter(f -> f.getId().equals(fieldId))
                    .findFirst()
                    .orElseThrow(() -> new ClientNotFoundException("Field not found with id: " + fieldId));
            field.setDisplayOrder(i);
        }

        fieldRepository.saveAll(fields);
        log.info("Reordered fields for client type {}", clientTypeId);
    }
}

