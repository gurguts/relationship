package org.example.containerservice.models.dto;

import lombok.Data;
import org.example.containerservice.models.dto.impl.IdNameDTO;

@Data
public class UserDTO implements IdNameDTO {
    private Long id;
    private String name;
}
