package org.example.clientservice.services.impl;

import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IClientTypeService {
    ClientType createClientType(ClientTypeCreateDTO dto);
    
    ClientType updateClientType(Long id, ClientTypeUpdateDTO dto);
    
    ClientType getClientTypeById(Long id);
    
    ClientType getClientTypeByIdWithFields(Long id);
    
    List<ClientType> getAllClientTypes();
    
    List<ClientType> getAllActiveClientTypes();
    
    Page<ClientType> getAllClientTypes(Pageable pageable);
    
    Page<ClientType> getAllActiveClientTypes(Pageable pageable);
    
    void deleteClientType(Long id);
    
    boolean existsByName(String name);
}

