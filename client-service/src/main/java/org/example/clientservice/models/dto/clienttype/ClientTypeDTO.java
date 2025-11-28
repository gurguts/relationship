package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientTypeDTO {
    private Long id;
    private String name;
    private String nameFieldLabel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ClientTypeFieldDTO> fields;
}

