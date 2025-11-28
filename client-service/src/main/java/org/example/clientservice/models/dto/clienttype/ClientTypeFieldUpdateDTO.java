package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ClientTypeFieldUpdateDTO {
    @Size(max = 255)
    private String fieldLabel;
    
    private Boolean isRequired;
    
    private Boolean isSearchable;
    
    private Boolean isFilterable;
    
    private Boolean isVisibleInTable;
    
    private Boolean isVisibleInCreate;
    
    private Integer displayOrder;
    
    @Size(max = 500)
    private String validationPattern;
    
    private Boolean allowMultiple;
    
    private List<String> listValues;
}

