package org.example.clientservice.models.dto.client;

import lombok.Data;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;

import java.util.List;

@Data
public class ClientDTO {
    private Long id;
    private String company;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
    private String sourceId;
    private List<ClientFieldValueDTO> fieldValues;
}