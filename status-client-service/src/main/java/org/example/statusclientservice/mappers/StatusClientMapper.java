package org.example.statusclientservice.mappers;

import org.example.statusclientservice.models.StatusClient;
import org.example.statusclientservice.models.dto.StatusClientCreateDTO;
import org.example.statusclientservice.models.dto.StatusClientDTO;
import org.example.statusclientservice.models.dto.StatusClientUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class StatusClientMapper {
    public StatusClient statusClientDTOToStatusClient(StatusClientDTO statusClientDTO) {
        StatusClient statusClient = new StatusClient();
        statusClient.setId(statusClientDTO.getId());
        statusClient.setName(statusClientDTO.getName());
        return statusClient;
    }

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
