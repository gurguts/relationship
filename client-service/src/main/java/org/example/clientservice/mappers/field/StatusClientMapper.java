package org.example.clientservice.mappers.field;

import org.example.clientservice.models.dto.fields.StatusClientCreateDTO;
import org.example.clientservice.models.dto.fields.StatusClientDTO;
import org.example.clientservice.models.dto.fields.StatusClientUpdateDTO;
import org.example.clientservice.models.field.StatusClient;
import org.springframework.stereotype.Component;

@Component
public class StatusClientMapper {

    public StatusClientDTO statusClientToStatusClientDTO(StatusClient statusClient) {
        StatusClientDTO statusClientDTO = new StatusClientDTO();
        statusClientDTO.setId(statusClient.getId());
        statusClientDTO.setName(statusClient.getName());
        return statusClientDTO;
    }

    public StatusClient statusClientCreateDTOToStatusClient(StatusClientCreateDTO dto) {
        StatusClient statusClient = new StatusClient();
        statusClient.setName(dto.getName());
        return statusClient;
    }

    public StatusClient statusClientUpdateDTOToStatusClient(StatusClientUpdateDTO dto) {
        StatusClient statusClient = new StatusClient();
        statusClient.setName(dto.getName());
        return statusClient;
    }
}
