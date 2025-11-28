package org.example.clientservice.services.impl;

import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;

import java.util.List;

public interface IClientTypeFieldService {
    ClientTypeField createField(Long clientTypeId, ClientTypeFieldCreateDTO dto);
    
    ClientTypeField updateField(Long fieldId, ClientTypeFieldUpdateDTO dto);
    
    ClientTypeField getFieldById(Long fieldId);
    
    List<ClientTypeField> getFieldsByClientTypeId(Long clientTypeId);
    
    List<ClientTypeField> getVisibleFieldsByClientTypeId(Long clientTypeId);
    
    List<ClientTypeField> getSearchableFieldsByClientTypeId(Long clientTypeId);
    
    List<ClientTypeField> getFilterableFieldsByClientTypeId(Long clientTypeId);
    
    List<ClientTypeField> getVisibleInCreateFieldsByClientTypeId(Long clientTypeId);
    
    void deleteField(Long fieldId);
    
    void reorderFields(Long clientTypeId, FieldReorderDTO dto);
}

