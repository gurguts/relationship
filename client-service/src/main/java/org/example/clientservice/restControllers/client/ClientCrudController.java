package org.example.clientservice.restControllers.client;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Validated
public class ClientCrudController {
    private final IClientCrudService clientService;
    private final ClientMapper clientMapper;

    @PreAuthorize("hasAuthority('client:create')")
    @PostMapping
    public ResponseEntity<ClientDTO> createClient(@RequestBody @Valid ClientCreateDTO clientCreateDTO) {
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
    public ResponseEntity<ClientDTO> updateClient(@RequestBody @Valid ClientUpdateDTO clientUpdateDTO,
                                                  @PathVariable Long clientId) {
        Client updateClient = clientMapper.clientUpdateDTOtoClient(clientUpdateDTO);
        Client updatedClient = clientService.updateClient(updateClient, clientId);
        ClientDTO updatedClientDTO = clientMapper.clientToClientDTO(updatedClient);
        return ResponseEntity.ok(updatedClientDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
        Client client = clientService.getClient(id);
        ClientDTO clientDTO = clientMapper.clientToClientDTO(client);
        return ResponseEntity.ok(clientDTO);
    }

    @PreAuthorize("hasAuthority('client:delete')")
    @DeleteMapping("/active/{id}")
    public ResponseEntity<Void> deleteClientById(@PathVariable Long id) {
        log.info("Deactivating client with ID: {}", id);
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('client:full_delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> fullDeleteClientById(@PathVariable Long id) {
        log.info("Fully deleting client with ID: {}", id);
        clientService.fullDeleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('client:edit')")
    @PatchMapping("/active/{id}")
    public ResponseEntity<Void> activateClientById(@PathVariable Long id) {
        log.info("Activating client with ID: {}", id);
        clientService.activateClient(id);
        return ResponseEntity.noContent().build();
    }
}
