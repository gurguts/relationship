package org.example.clientservice.services.clienttype;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.mappers.clienttype.ClientFieldValueMapper;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.clientservice.repositories.clienttype.ClientFieldValueRepository;
import org.example.clientservice.services.impl.IClientFieldValueService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientFieldValueService implements IClientFieldValueService {
    private static final int MAX_CLIENT_IDS_LIMIT = 1000;
    
    private final ClientFieldValueRepository fieldValueRepository;
    private final ClientFieldValueMapper fieldValueMapper;

    @Override
    @NonNull
    public List<ClientFieldValueDTO> getFieldValuesByClientId(@NonNull Long clientId) {
        log.info("Getting field values for client with ID: {}", clientId);
        
        try {
            List<ClientFieldValue> fieldValues = fieldValueRepository.findByClientIdOrderByDisplayOrderAsc(clientId);
            
            return fieldValues.stream()
                    .map(fieldValueMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting field values for client ID {}: {}", clientId, e.getMessage(), e);
            throw new ClientException("FIELD_VALUES_FETCH_ERROR",
                    String.format("Failed to get field values for client ID %d: %s", clientId, e.getMessage()), e);
        }
    }

    @Override
    @NonNull
    public Map<Long, List<ClientFieldValueDTO>> getFieldValuesByClientIds(@NonNull List<Long> clientIds) {
        validateClientIds(clientIds);
        
        try {
            List<ClientFieldValue> allFieldValues = fieldValueRepository.findByClientIdInWithFieldAndValueList(clientIds);
            
            if (allFieldValues.isEmpty()) {
                return Collections.emptyMap();
            }
            
            return allFieldValues.stream()
                    .filter(fv -> fv.getClient().getId() != null)
                    .collect(Collectors.groupingBy(
                            fv -> fv.getClient().getId(),
                            Collectors.mapping(
                                    fieldValueMapper::toDTO,
                                    Collectors.toList()
                            )
                    ));
        } catch (Exception e) {
            log.error("Error getting field values for {} clients: {}", clientIds.size(), e.getMessage(), e);
            throw new ClientException("FIELD_VALUES_BATCH_FETCH_ERROR",
                    String.format("Failed to get field values for clients: %s", e.getMessage()), e);
        }
    }
    
    private void validateClientIds(@NonNull List<Long> clientIds) {
        if (clientIds.isEmpty()) {
            throw new ClientException("INVALID_CLIENT_IDS", "List of client IDs cannot be empty");
        }
        
        if (clientIds.size() > MAX_CLIENT_IDS_LIMIT) {
            throw new ClientException("INVALID_CLIENT_IDS",
                    String.format("Cannot fetch field values for more than %d clients at once", MAX_CLIENT_IDS_LIMIT));
        }
        
        if (clientIds.contains(null)) {
            throw new ClientException("INVALID_CLIENT_IDS", "List of client IDs cannot contain null values");
        }
    }
}
