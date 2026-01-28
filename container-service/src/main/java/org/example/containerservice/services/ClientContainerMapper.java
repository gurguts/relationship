package org.example.containerservice.services;

import lombok.NonNull;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.container.ClientContainerResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ClientContainerMapper {

    public ClientContainerResponseDTO mapToClientContainerPageDTO(@NonNull ClientContainer clientContainer,
                                                                 @NonNull Map<Long, ClientDTO> clientMap) {
        ClientContainerResponseDTO dto = new ClientContainerResponseDTO();
        dto.setUserId(clientContainer.getUser());
        dto.setClient(clientMap.get(clientContainer.getClient()));
        dto.setContainerId(clientContainer.getContainer().getId());
        dto.setContainerName(clientContainer.getContainer().getName());
        dto.setQuantity(clientContainer.getQuantity());
        dto.setUpdatedAt(clientContainer.getUpdatedAt());
        return dto;
    }
}
