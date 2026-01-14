package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IClientTypeService {
    @NonNull
    ClientType createClientType(@NonNull ClientTypeCreateDTO dto);
    
    @NonNull
    ClientType updateClientType(@NonNull Long id, @NonNull ClientTypeUpdateDTO dto);
    
    @NonNull
    ClientType getClientTypeById(@NonNull Long id);
    
    @NonNull
    ClientType getClientTypeByIdWithFields(@NonNull Long id);

    @NonNull
    List<ClientType> getAllActiveClientTypes();
    
    @NonNull
    Page<ClientType> getAllClientTypes(@NonNull Pageable pageable);
    
    @NonNull
    Page<ClientType> getAllActiveClientTypes(@NonNull Pageable pageable);
    
    void deleteClientType(@NonNull Long id);

    @NonNull
    ClientTypeFieldListValue getListValueById(@NonNull Long id);

    @NonNull
    StaticFieldsConfig getStaticFieldsConfig(@NonNull Long id);
    
    @NonNull
    StaticFieldsConfig updateStaticFieldsConfig(@NonNull Long id, @NonNull StaticFieldsConfig config);
}

