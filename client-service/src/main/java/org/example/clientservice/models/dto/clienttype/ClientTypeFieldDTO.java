package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

import java.util.List;

@Data
public class ClientTypeFieldDTO {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private Boolean isRequired;
    private Boolean isSearchable;
    private Boolean isFilterable;
    private Boolean isVisibleInTable;
    private Boolean isVisibleInCreate;
    private Integer displayOrder;
    private Integer columnWidth;
    private String validationPattern;
    private Boolean allowMultiple;
    private List<ClientTypeFieldListValueDTO> listValues;
}

