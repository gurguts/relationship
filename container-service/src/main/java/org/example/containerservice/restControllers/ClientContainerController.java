package org.example.containerservice.restControllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.dto.container.ClientContainerDTO;
import org.example.containerservice.models.dto.container.CollectFromClientRequest;
import org.example.containerservice.models.dto.container.TransferToClientRequest;
import org.example.containerservice.services.ClientContainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/containers/client")
@RequiredArgsConstructor
@Validated
public class ClientContainerController {

    private final ClientContainerService clientContainerService;
    private final ContainerMapper containerMapper;

    @PreAuthorize("hasAuthority('container:transfer')")
    @PostMapping("/transfer")
    public ResponseEntity<Void> transferContainerToClient(@RequestBody @Valid @NonNull TransferToClientRequest request) {
        clientContainerService.transferContainerToClient(
                request.getUserId(), request.getClientId(), request.getContainerId(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('container:transfer')")
    @PostMapping("/collect")
    public ResponseEntity<Void> collectContainerFromClient(@RequestBody @Valid @NonNull CollectFromClientRequest request) {
        clientContainerService.collectContainerFromClient(
                request.getUserId(), request.getClientId(), request.getContainerId(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('container:view')")
    @GetMapping("/{clientId}")
    public ResponseEntity<List<ClientContainerDTO>> getClientContainers(@PathVariable @Positive Long clientId) {
        List<ClientContainerDTO> containers = clientContainerService.getClientContainers(clientId)
                .stream()
                .map(containerMapper::toClientContainerDTO)
                .toList();
        return ResponseEntity.ok(containers);
    }
}
