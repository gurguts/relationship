package org.example.clientservice.services.clienttype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.mappers.clienttype.ClientTypeMapper;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
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
    private final ClientTypeRepository clientTypeRepository;
    private final ClientRepository clientRepository;
    private final ClientTypeMapper clientTypeMapper;
    private final ClientTypeFieldListValueRepository listValueRepository;

    @Override
    @Transactional
    public ClientType createClientType(ClientTypeCreateDTO dto) {
        if (existsByName(dto.getName())) {
            throw new ClientException("Client type with name " + dto.getName() + " already exists");
        }

        ClientType clientType = clientTypeMapper.createDTOToClientType(dto);
        log.info("Creating client type: {}", dto.getName());
        return clientTypeRepository.save(clientType);
    }

    @Override
    @Transactional
    public ClientType updateClientType(Long id, ClientTypeUpdateDTO dto) {
        ClientType clientType = getClientTypeById(id);

        if (dto.getName() != null && !dto.getName().equals(clientType.getName())) {
            if (existsByName(dto.getName())) {
                throw new ClientException("Client type with name " + dto.getName() + " already exists");
            }
        }

        clientTypeMapper.updateClientTypeFromDTO(clientType, dto);
        log.info("Updating client type with ID: {}", id);
        return clientTypeRepository.save(clientType);
    }

    @Override
    public ClientType getClientTypeById(Long id) {
        return clientTypeRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client type not found with id: " + id));
    }

    @Override
    public ClientType getClientTypeByIdWithFields(Long id) {
        ClientType clientType = getClientTypeById(id);
        clientType.getFields().size();
        return clientType;
    }

    @Override
    public List<ClientType> getAllClientTypes() {
        return clientTypeRepository.findAll();
    }

    @Override
    public List<ClientType> getAllActiveClientTypes() {
        return clientTypeRepository.findAllActiveOrderedByName();
    }

    @Override
    public Page<ClientType> getAllClientTypes(Pageable pageable) {
        return clientTypeRepository.findAll(pageable);
    }

    @Override
    public Page<ClientType> getAllActiveClientTypes(Pageable pageable) {
        return clientTypeRepository.findByIsActiveTrue(pageable);
    }

    @Override
    @Transactional
    public void deleteClientType(Long id) {
        ClientType clientType = getClientTypeById(id);
        
        long clientCount = clientRepository.count();
        if (clientCount > 0) {
            throw new ClientException("Cannot delete client type. There are clients associated with this type.");
        }

        log.info("Deleting client type with ID: {}", id);
        clientTypeRepository.delete(clientType);
    }

    @Override
    public boolean existsByName(String name) {
        return clientTypeRepository.findByName(name).isPresent();
    }

    public ClientTypeFieldListValue getListValueById(Long id) {
        return listValueRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("List value not found with id: " + id));
    }
}

