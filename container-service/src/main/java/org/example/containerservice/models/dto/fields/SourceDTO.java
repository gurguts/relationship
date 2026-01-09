package org.example.containerservice.models.dto.fields;

import lombok.Data;
import org.example.containerservice.models.dto.impl.IdNameDTO;

@Data
public class SourceDTO implements IdNameDTO {
    private Long id;
    private String name;
}
