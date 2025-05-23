package org.example.containerservice.restControllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.clients.UserApiClient;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.models.dto.container.ContainerBalanceDTO;
import org.example.containerservice.models.dto.container.ContainerOperationRequest;
import org.example.containerservice.models.dto.container.UserContainerBalanceDTO;
import org.example.containerservice.models.dto.UserDTO;
import org.example.containerservice.services.ContainerBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/containers/balance")
@RequiredArgsConstructor
public class ContainerBalanceController {

    private final ContainerBalanceService containerBalanceService;
    private final ContainerMapper containerMapper;
    private final UserApiClient userApiClient;

    @PreAuthorize("hasAuthority('container:view')")
    @GetMapping("/users")
    public ResponseEntity<List<UserContainerBalanceDTO>> getAllUserContainerBalances() {
        log.info("Fetching container balances for all users");

        List<UserDTO> allUsers = userApiClient.getAllUsers();

        Map<Long, List<ContainerBalance>> balancesByUser = containerBalanceService.getAllUserContainerBalances();

        List<UserContainerBalanceDTO> balances = allUsers.stream()
                .map(user -> {
                    UserContainerBalanceDTO dto = new UserContainerBalanceDTO();
                    dto.setUserId(user.getId());
                    dto.setUserName(user.getName());
                    List<ContainerBalance> userBalances = balancesByUser.getOrDefault(user.getId(), new ArrayList<>());
                    dto.setBalances(userBalances.stream()
                            .map(containerMapper::toContainerBalanceDetailDTO)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(balances);
    }

    @PreAuthorize("hasAuthority('container:balance')")
    @PostMapping("/deposit")
    public ResponseEntity<Void> depositContainer(@RequestBody ContainerOperationRequest request) {
        log.info("Received deposit request: {}", request);
        containerBalanceService.depositContainer(request.getUserId(), request.getContainerId(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('container:balance')")
    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdrawContainer(@RequestBody ContainerOperationRequest request) {
        log.info("Received withdraw request: {}", request);
        containerBalanceService.withdrawContainer(request.getUserId(), request.getContainerId(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ContainerBalanceDTO>> getUserContainerBalances(@PathVariable Long userId) {
        log.info("Fetching container balances for user: {}", userId);
        List<ContainerBalanceDTO> balances = containerBalanceService.getUserContainerBalances(userId)
                .stream()
                .map(containerMapper::toContainerBalanceDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(balances);
    }
}