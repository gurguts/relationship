package org.example.containerservice.models.dto.clienttype;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClientFieldValueDTO {
    private Long id;
    private Long fieldId;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String valueText;
    private BigDecimal valueNumber;
    private LocalDate valueDate;
    private Boolean valueBoolean;
    private Long valueListId;
    private String valueListValue;
    private Integer displayOrder;
}
