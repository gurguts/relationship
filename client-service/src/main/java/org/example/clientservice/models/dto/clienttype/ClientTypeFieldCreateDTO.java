package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ClientTypeFieldCreateDTO {
    @NotBlank
    @Size(max = 255)
    private String fieldName;
    
    @NotBlank
    @Size(max = 255)
    private String fieldLabel;
    
    @NotNull
    private String fieldType;
    
    private Boolean isRequired = false;
    
    private Boolean isSearchable = true;
    
    private Boolean isFilterable = false;
    
    private Boolean isVisibleInTable = true;
    
    private Boolean isVisibleInCreate = true;
    
    private Integer displayOrder = 0;
    
    private Integer columnWidth;
    
    @Size(max = 500)
    private String validationPattern;
    
    private Boolean allowMultiple = false;
    
    private List<String> listValues;
}

