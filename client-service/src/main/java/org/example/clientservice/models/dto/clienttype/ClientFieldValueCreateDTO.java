package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClientFieldValueCreateDTO {
    private Long fieldId;
    private String valueText;
    private BigDecimal valueNumber;
    private LocalDate valueDate;
    private Boolean valueBoolean;
    private Long valueListId;
    private Integer displayOrder;
}

