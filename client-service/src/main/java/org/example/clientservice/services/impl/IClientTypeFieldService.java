package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldsAllDTO;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;

import java.util.List;

public interface IClientTypeFieldService {
    @NonNull
    ClientTypeField createField(@NonNull Long clientTypeId, @NonNull ClientTypeFieldCreateDTO dto);
    
    @NonNull
    ClientTypeField updateField(@NonNull Long fieldId, @NonNull ClientTypeFieldUpdateDTO dto);
    
    @NonNull
    ClientTypeField getFieldById(@NonNull Long fieldId);
    
    @NonNull
    List<ClientTypeField> getFieldsByClientTypeId(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypeField> getVisibleFieldsByClientTypeId(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypeFieldDTO> getVisibleFieldsWithStatic(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypeField> getSearchableFieldsByClientTypeId(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypeField> getFilterableFieldsByClientTypeId(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypeField> getVisibleInCreateFieldsByClientTypeId(@NonNull Long clientTypeId);
    
    @NonNull
    List<ClientTypeField> getFieldsByIds(@NonNull FieldIdsRequest request);
    
    void deleteField(@NonNull Long fieldId);
    
    void reorderFields(@NonNull Long clientTypeId, @NonNull FieldReorderDTO dto);
    
    @NonNull
    ClientTypeFieldsAllDTO getAllFieldsByClientTypeId(@NonNull Long clientTypeId);
}

