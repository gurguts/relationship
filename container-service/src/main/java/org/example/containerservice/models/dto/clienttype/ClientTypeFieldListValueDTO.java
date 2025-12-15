package org.example.containerservice.models.dto.clienttype;

import lombok.Data;

@Data
public class ClientTypeFieldListValueDTO {
    private Long id;
    private Long fieldId;
    private String value;
    private Integer displayOrder;
}

