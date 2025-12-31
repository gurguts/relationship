package org.example.clientservice.restControllers.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.mappers.ClientMapper;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.dto.client.ClientCreateDTO;
import org.example.clientservice.models.dto.client.ClientDTO;
import org.example.clientservice.models.dto.client.ClientUpdateDTO;
import org.example.clientservice.services.impl.IClientCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Validated
public class ClientCrudController {
    private final IClientCrudService clientService;
    private final ClientMapper clientMapper;

    @PreAuthorize("hasAuthority('client:create')")
    @PostMapping
    public ResponseEntity<ClientDTO> createClient(@RequestBody @Valid @NonNull ClientCreateDTO clientCreateDTO) {
        Client client = clientMapper.clientCreateDTOToClient(clientCreateDTO);
        Client createdClient = clientService.createClient(client);
        ClientDTO createdClientDTO = clientMapper.clientToClientDTO(createdClient);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClient.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdClientDTO);
    }

    @PreAuthorize("hasAuthority('client:edit')")
    @PatchMapping("/{clientId}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable @Positive Long clientId,
                                                  @RequestBody @Valid @NonNull ClientUpdateDTO clientUpdateDTO) {
        Client updateClient = clientMapper.clientUpdateDTOtoClient(clientUpdateDTO);
        Client updatedClient = clientService.updateClient(updateClient, clientId);
        ClientDTO updatedClientDTO = clientMapper.clientToClientDTO(updatedClient);
        return ResponseEntity.ok(updatedClientDTO);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable @Positive Long clientId) {
        Client client = clientService.getClient(clientId);
        ClientDTO clientDTO = clientMapper.clientToClientDTO(client);
        return ResponseEntity.ok(clientDTO);
    }

    @PreAuthorize("hasAuthority('client:delete')")
    @DeleteMapping("/active/{clientId}")
    public ResponseEntity<Void> deleteClientById(@PathVariable @Positive Long clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('client:full_delete')")
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> fullDeleteClientById(@PathVariable @Positive Long clientId) {
        clientService.fullDeleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('client:edit')")
    @PatchMapping("/active/{clientId}")
    public ResponseEntity<Void> activateClientById(@PathVariable @Positive Long clientId) {
        clientService.activateClient(clientId);
        return ResponseEntity.noContent().build();
    }
}
