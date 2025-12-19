package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

import java.util.List;

@Data
public class ClientTypeFieldsAllDTO {
    private List<ClientTypeFieldDTO> all;
    private List<ClientTypeFieldDTO> visible;
    private List<ClientTypeFieldDTO> searchable;
    private List<ClientTypeFieldDTO> filterable;
    private List<ClientTypeFieldDTO> visibleInCreate;
}

