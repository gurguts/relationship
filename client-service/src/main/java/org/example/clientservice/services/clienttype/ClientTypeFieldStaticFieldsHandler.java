package org.example.clientservice.services.clienttype;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldsAllDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;
import org.example.clientservice.services.impl.IClientTypeService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientTypeFieldStaticFieldsHandler {

    private static final int DEFAULT_DISPLAY_ORDER = 999;

    private final IClientTypeService clientTypeService;

    public void addStaticFieldsToResponse(@NonNull List<ClientTypeFieldDTO> response, @NonNull Long clientTypeId) {
        try {
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
            addStaticFields(response, clientType);
        } catch (Exception e) {
            log.warn("Failed to add static fields for client type {}: {}", clientTypeId, e.getMessage());
        }
    }

    public void addStaticFieldsToVisible(@NonNull ClientTypeFieldsAllDTO dto, @NonNull ClientType clientType) {
        try {
            addStaticFields(dto.getVisible(), clientType);
        } catch (Exception e) {
            log.warn("Failed to add static fields to visible: {}", e.getMessage());
        }
    }

    private void addStaticFields(@NonNull List<ClientTypeFieldDTO> fields, @NonNull ClientType clientType) {
        StaticFieldsConfig staticConfig = StaticFieldsHelper.parseStaticFieldsConfig(clientType);
        
        if (staticConfig != null) {
            List<ClientTypeFieldDTO> staticFields = StaticFieldsHelper.createStaticFieldDTOs(staticConfig);
            fields.addAll(staticFields);
            sortFieldsByDisplayOrder(fields);
        }
    }

    private void sortFieldsByDisplayOrder(@NonNull List<ClientTypeFieldDTO> fields) {
        fields.sort(Comparator.comparingInt(field -> 
                field.getDisplayOrder() != null ? field.getDisplayOrder() : DEFAULT_DISPLAY_ORDER));
    }
}
