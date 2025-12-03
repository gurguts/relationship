package org.example.clientservice.models.dto.client;

import lombok.Data;

@Data
public class ClientDTO {
    private Long id;
    private String company;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
    private String sourceId;
}