package org.example.clientservice.models.dto.fields;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProductDTO {
    private long id;
    private String name;
}
