package org.example.clientservice.models.dto.client;

import lombok.Data;
import org.example.clientservice.models.dto.fields.SourceDTO;

@Data
public class ClientListDTO {
    private Long id;
    private String company;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
    private SourceDTO source;
}