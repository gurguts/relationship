package org.example.clientservice.services.clienttype;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;
import org.example.clientservice.services.impl.IClientTypeService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientTypeFieldValidator {

    private final IClientTypeService clientTypeService;

    public void validateFieldIdsRequest(@NonNull FieldIdsRequest request) {
        if (request.fieldIds().isEmpty()) {
            throw new ClientException("INVALID_FIELD_IDS", "List of field IDs cannot be empty");
        }
    }

    public void validateReorderRequest(@NonNull Long clientTypeId, @NonNull FieldReorderDTO dto) {
        if (dto.getFieldIds() == null || dto.getFieldIds().isEmpty()) {
            throw new ClientException("INVALID_FIELD_IDS", "List of field IDs for reordering cannot be empty");
        }
        
        if (dto.getFieldIds().contains(null)) {
            throw new ClientException("INVALID_FIELD_IDS", "List of field IDs cannot contain null values");
        }
        
        clientTypeService.getClientTypeById(clientTypeId);
    }
}
