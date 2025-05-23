package org.example.clientservice.models.dto.fields;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.clientservice.models.dto.impl.IdNameDTO;

@Data
@AllArgsConstructor
public class StatusDTO implements IdNameDTO {
    private Long id;
    private String name;
}
