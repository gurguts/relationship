package org.example.purchaseservice.models.dto.user;

import lombok.Data;
import org.example.purchaseservice.models.dto.impl.IdNameDTO;

@Data
public class UserDTO implements IdNameDTO {
    private Long id;
    private String name;
}
