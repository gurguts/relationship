package org.example.containerservice.mappers;

import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.models.ContainerTransaction;
import org.example.containerservice.models.dto.container.*;
import org.springframework.stereotype.Component;

@Component
public class ContainerMapper {

    public ContainerDTO containerToContainerDTO(Container container) {
        if (container == null) {
            return null;
        }

        ContainerDTO dto = new ContainerDTO();
        dto.setId(container.getId());
        dto.setName(container.getName());
        return dto;
    }

    public ContainerBalanceDTO toContainerBalanceDTO(ContainerBalance balance) {
        ContainerBalanceDTO dto = new ContainerBalanceDTO();
        dto.setUserId(balance.getUserId());
        dto.setContainerId(balance.getContainer().getId());
        dto.setContainerName(balance.getContainer().getName());
        dto.setTotalQuantity(balance.getTotalQuantity());
        dto.setClientQuantity(balance.getClientQuantity());
        return dto;
    }

    public ClientContainerDTO toClientContainerDTO(ClientContainer clientContainer) {
        ClientContainerDTO dto = new ClientContainerDTO();
        dto.setClientId(clientContainer.getClient());
        dto.setUserId(clientContainer.getUser());
        dto.setContainerId(clientContainer.getContainer().getId());
        dto.setContainerName(clientContainer.getContainer().getName());
        dto.setQuantity(clientContainer.getQuantity());
        dto.setUpdatedAt(clientContainer.getUpdatedAt());
        return dto;
    }

    public ContainerTransactionDTO toContainerTransactionDTO(ContainerTransaction transaction) {
        ContainerTransactionDTO dto = new ContainerTransactionDTO();
        dto.setFromUserId(transaction.getFromUserId());
        dto.setToUserId(transaction.getToUserId());
        dto.setClientId(transaction.getClientId());
        dto.setContainerId(transaction.getContainer().getId());
        dto.setContainerName(transaction.getContainer().getName());
        dto.setQuantity(transaction.getQuantity());
        dto.setType(transaction.getType());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }

    public ContainerBalanceDetailDTO toContainerBalanceDetailDTO(ContainerBalance balance) {
        ContainerBalanceDetailDTO dto = new ContainerBalanceDetailDTO();
        dto.setContainerId(balance.getContainer().getId());
        dto.setContainerName(balance.getContainer().getName());
        dto.setTotalQuantity(balance.getTotalQuantity());
        dto.setClientQuantity(balance.getClientQuantity());
        return dto;
    }

    public Container containerCreateDTOToContainer(ContainerCreateDTO dto) {
        Container container = new Container();
        container.setName(dto.getName());
        return container;
    }

    public Container containerUpdateDTOToContainer(ContainerUpdateDTO dto) {
        Container container = new Container();
        container.setName(dto.getName());
        return container;
    }
}