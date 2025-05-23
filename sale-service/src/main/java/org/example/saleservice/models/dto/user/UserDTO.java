package org.example.saleservice.models.dto.user;

import lombok.Data;
import org.example.saleservice.models.dto.impl.IdNameDTO;

@Data
public class UserDTO implements IdNameDTO {
    private Long id;
    private String name;
}
