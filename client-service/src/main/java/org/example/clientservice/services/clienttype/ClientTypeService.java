package org.example.clientservice.services.clienttype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.mappers.clienttype.ClientTypeMapper;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;
import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.repositories.clienttype.ClientTypeFieldListValueRepository;
import org.example.clientservice.repositories.clienttype.ClientTypeRepository;
import org.example.clientservice.services.impl.IClientTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTypeService implements IClientTypeService {
    private static final String ERROR_CLIENT_TYPE_ALREADY_EXISTS = "CLIENT_TYPE_ALREADY_EXISTS";
    private static final String ERROR_INVALID_ID = "INVALID_ID";
    private static final String ERROR_INVALID_NAME = "INVALID_NAME";
    private static final String ERROR_DELETE_FORBIDDEN = "DELETE_FORBIDDEN";
    private static final String ERROR_SERIALIZATION_ERROR = "SERIALIZATION_ERROR";
    private static final String ERROR_CLIENT_TYPE_CREATION = "CLIENT_TYPE_CREATION_ERROR";
    private static final String ERROR_CLIENT_TYPE_UPDATE = "CLIENT_TYPE_UPDATE_ERROR";
    private static final String ERROR_CLIENT_TYPE_DELETION = "CLIENT_TYPE_DELETION_ERROR";
    private static final String ERROR_STATIC_FIELDS_CONFIG_UPDATE = "STATIC_FIELDS_CONFIG_UPDATE_ERROR";
    
    private final ClientTypeRepository clientTypeRepository;
    private final ClientRepository clientRepository;
    private final ClientTypeMapper clientTypeMapper;
    private final ClientTypeFieldListValueRepository listValueRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @NonNull
    public ClientType createClientType(@NonNull ClientTypeCreateDTO dto) {
        log.info("Creating client type: {}", dto.getName());
        
        try {
            validateClientTypeName(dto.getName());
            validateNameUniqueness(dto.getName(), null);
            
            ClientType clientType = clientTypeMapper.createDTOToClientType(dto);
            return clientTypeRepository.save(clientType);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating client type {}: {}", dto.getName(), e.getMessage(), e);
            throw new ClientException(ERROR_CLIENT_TYPE_CREATION,
                    String.format("Failed to create client type: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    @NonNull
    public ClientType updateClientType(@NonNull Long id, @NonNull ClientTypeUpdateDTO dto) {
        log.info("Updating client type with ID: {}", id);
        
        try {
            validateId(id);
            ClientType clientType = getClientTypeById(id);
            
            if (dto.getName() != null && !dto.getName().equals(clientType.getName())) {
                validateClientTypeName(dto.getName());
                validateNameUniqueness(dto.getName(), id);
            }
            
            clientTypeMapper.updateClientTypeFromDTO(clientType, dto);
            return clientTypeRepository.save(clientType);
        } catch (ClientNotFoundException | ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating client type with ID {}: {}", id, e.getMessage(), e);
            throw new ClientException(ERROR_CLIENT_TYPE_UPDATE,
                    String.format("Failed to update client type: %s", e.getMessage()), e);
        }
    }

    @Override
    @NonNull
    public ClientType getClientTypeById(@NonNull Long id) {
        validateId(id);
        return clientTypeRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(
                        String.format("Client type not found with id: %d", id)));
    }

    @Override
    @NonNull
    public ClientType getClientTypeByIdWithFields(@NonNull Long id) {
        validateId(id);
        return clientTypeRepository.findByIdWithFields(id)
                .orElseThrow(() -> new ClientNotFoundException(
                        String.format("Client type not found with id: %d", id)));
    }

    @Override
    @NonNull
    public List<ClientType> getAllActiveClientTypes() {
        return clientTypeRepository.findAllActiveOrderedByName();
    }

    @Override
    @NonNull
    public Page<ClientType> getAllClientTypes(@NonNull Pageable pageable) {
        return clientTypeRepository.findAll(pageable);
    }

    @Override
    @NonNull
    public Page<ClientType> getAllActiveClientTypes(@NonNull Pageable pageable) {
        return clientTypeRepository.findByIsActiveTrue(pageable);
    }

    @Override
    @Transactional
    public void deleteClientType(@NonNull Long id) {
        log.info("Deleting client type with ID: {}", id);
        
        try {
            validateId(id);
            ClientType clientType = getClientTypeById(id);
            
            long clientCount = clientRepository.countByClientTypeId(id);
            if (clientCount > 0) {
                throw new ClientException(ERROR_DELETE_FORBIDDEN, 
                        "Cannot delete client type. There are clients associated with this type.");
            }
            
            clientTypeRepository.delete(clientType);
        } catch (ClientNotFoundException | ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting client type with ID {}: {}", id, e.getMessage(), e);
            throw new ClientException(ERROR_CLIENT_TYPE_DELETION,
                    String.format("Failed to delete client type: %s", e.getMessage()), e);
        }
    }

    @NonNull
    public ClientTypeFieldListValue getListValueById(@NonNull Long id) {
        validateId(id);
        return listValueRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(
                        String.format("List value not found with id: %d", id)));
    }

    @Override
    @NonNull
    public StaticFieldsConfig getStaticFieldsConfig(@NonNull Long id) {
        validateId(id);
        ClientType clientType = getClientTypeById(id);
        StaticFieldsConfig config = StaticFieldsHelper.parseStaticFieldsConfig(clientType);
        return config != null ? config : new StaticFieldsConfig();
    }

    @Override
    @Transactional
    @NonNull
    public StaticFieldsConfig updateStaticFieldsConfig(@NonNull Long id, @NonNull StaticFieldsConfig config) {
        log.info("Updating static fields config for client type with ID: {}", id);
        
        try {
            validateId(id);
            ClientType clientType = getClientTypeById(id);
            
            String configJson = objectMapper.writeValueAsString(config);
            clientType.setStaticFieldsConfig(configJson);
            clientTypeRepository.save(clientType);
            return config;
        } catch (ClientNotFoundException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Error serializing static fields config for client type with ID {}: {}", id, e.getMessage(), e);
            throw new ClientException(ERROR_SERIALIZATION_ERROR,
                    String.format("Failed to serialize static fields config: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Error updating static fields config for client type with ID {}: {}", id, e.getMessage(), e);
            throw new ClientException(ERROR_STATIC_FIELDS_CONFIG_UPDATE,
                    String.format("Failed to update static fields config: %s", e.getMessage()), e);
        }
    }
    
    private void validateId(@NonNull Long id) {
        if (id <= 0) {
            throw new ClientException(ERROR_INVALID_ID, "ID must be positive");
        }
    }
    
    private void validateClientTypeName(@NonNull String name) {
        if (name.trim().isEmpty()) {
            throw new ClientException(ERROR_INVALID_NAME, "Client type name cannot be null or empty");
        }
    }
    
    private void validateNameUniqueness(@NonNull String name, Long excludeId) {
        clientTypeRepository.findByName(name)
                .ifPresent(existingClientType -> {
                    if (!existingClientType.getId().equals(excludeId)) {
                        throw new ClientException(ERROR_CLIENT_TYPE_ALREADY_EXISTS,
                                String.format("Client type with name %s already exists", name));
                    }
                });
    }
}
