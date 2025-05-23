package org.example.containerservice.restControllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.services.ClientContainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.dto.container.TransferToClientRequest;
import org.example.containerservice.models.dto.container.CollectFromClientRequest;
import org.example.containerservice.models.dto.container.ClientContainerDTO;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/containers/client")
@RequiredArgsConstructor
public class ClientContainerController {

    private final ClientContainerService clientContainerService;
    private final ContainerMapper containerMapper;

    @PreAuthorize("hasAuthority('container:transfer')")
    @PostMapping("/transfer")
    public ResponseEntity<Void> transferContainerToClient(@RequestBody TransferToClientRequest request) {
        log.info("Received transfer request: {}", request);
        clientContainerService.transferContainerToClient(
                request.getClientId(), request.getContainerId(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('container:transfer')")
    @PostMapping("/collect")
    public ResponseEntity<Void> collectContainerFromClient(@RequestBody CollectFromClientRequest request) {
        log.info("Received collect request: {}", request);
        clientContainerService.collectContainerFromClient(
                request.getClientId(), request.getContainerId(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('container:view')")
    @GetMapping("/{clientId}")
    public ResponseEntity<List<ClientContainerDTO>> getClientContainers(@PathVariable Long clientId) {
        log.info("Fetching containers for client: {}", clientId);
        List<ClientContainerDTO> containers = clientContainerService.getClientContainers(clientId)
                .stream()
                .map(containerMapper::toClientContainerDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(containers);
    }
}
