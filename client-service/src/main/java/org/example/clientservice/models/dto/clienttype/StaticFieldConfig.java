package org.example.clientservice.models.dto.clienttype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticFieldConfig {
    private Boolean isVisible;
    private Integer displayOrder;
    private Integer columnWidth;
    private String fieldLabel;
}

