package org.example.containerservice.models.dto.container;

import lombok.Data;

import java.util.List;

@Data
public class UserContainerBalanceDTO {
    private Long userId;
    private String userName;
    private List<ContainerBalanceDetailDTO> balances;
}