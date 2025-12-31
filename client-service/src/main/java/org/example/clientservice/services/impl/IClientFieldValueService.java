package org.example.clientservice.services.impl;

import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;

import java.util.List;
import java.util.Map;

public interface IClientFieldValueService {
    List<ClientFieldValueDTO> getFieldValuesByClientId(Long clientId);
    
    Map<Long, List<ClientFieldValueDTO>> getFieldValuesByClientIds(List<Long> clientIds);
}

