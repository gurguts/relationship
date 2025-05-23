package org.example.purchaseservice.models.dto.fields;

import lombok.Data;
import org.example.purchaseservice.models.dto.impl.IdNameDTO;

@Data
public class BusinessDTO implements IdNameDTO {
    private Long id;
    private String name;
}
