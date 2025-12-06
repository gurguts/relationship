package org.example.clientservice.models.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueCreateDTO;

import java.util.List;

@Data
public class ClientCreateDTO {

    @NotNull(message = "{validation.clientTypeId.notnull}")
    private Long clientTypeId;

    @NotBlank(message = "{validation.company.notblank}")
    @Size(max = 255, message = "{validation.company.size}")
    private String company;

    private Long sourceId;

    private List<ClientFieldValueCreateDTO> fieldValues;
}