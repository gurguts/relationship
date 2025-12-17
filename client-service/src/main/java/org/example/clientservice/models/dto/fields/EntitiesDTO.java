package org.example.clientservice.models.dto.fields;

import lombok.Data;
import org.example.clientservice.models.dto.product.ProductDTO;
import org.example.clientservice.models.dto.user.UserDTO;

import java.util.List;

@Data
public class EntitiesDTO {
    private List<SourceDTO> sources;
    private List<UserDTO> users;
    private List<ProductDTO> products;
}