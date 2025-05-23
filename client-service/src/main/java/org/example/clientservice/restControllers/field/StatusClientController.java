package org.example.clientservice.restControllers.field;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.field.StatusClientMapper;
import org.example.clientservice.models.dto.fields.StatusClientCreateDTO;
import org.example.clientservice.models.dto.fields.StatusClientDTO;
import org.example.clientservice.models.dto.fields.StatusClientUpdateDTO;
import org.example.clientservice.models.field.StatusClient;
import org.example.clientservice.services.impl.IStatusClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
@Slf4j
public class StatusClientController {
    private final IStatusClientService statusClientService;
    private final StatusClientMapper statusClientMapper;

    @GetMapping("/{id}")
    public ResponseEntity<StatusClientDTO> getStatusClient(@PathVariable Long id) {
        StatusClient statusClient = statusClientService.getStatusClient(id);
        return ResponseEntity.ok(statusClientMapper.statusClientToStatusClientDTO(statusClient));
    }

    @GetMapping
    public ResponseEntity<List<StatusClientDTO>> getAllStatusClients() {
        List<StatusClient> statusClients = statusClientService.getAllStatusClients();
        List<StatusClientDTO> dtos = statusClients.stream()
                .map(statusClientMapper::statusClientToStatusClientDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<StatusClientDTO> createStatusClient(
            @RequestBody @Valid StatusClientCreateDTO statusClientCreateDTO) {
        StatusClient statusClient = statusClientMapper.statusClientCreateDTOToStatusClient(statusClientCreateDTO);
        StatusClientDTO createdStatusClient = statusClientMapper.statusClientToStatusClientDTO(
                statusClientService.createStatusClient(statusClient));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdStatusClient.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdStatusClient);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<StatusClientDTO> updateStatusClient(@PathVariable Long id,
                                                              @RequestBody @Valid
                                                              StatusClientUpdateDTO statusClientUpdateDTO) {
        StatusClient statusClient = statusClientMapper.statusClientUpdateDTOToStatusClient(statusClientUpdateDTO);
        StatusClient updatedStatusClient = statusClientService.updateStatusClient(id, statusClient);
        return ResponseEntity.ok(statusClientMapper.statusClientToStatusClientDTO(updatedStatusClient));
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatusClient(@PathVariable Long id) {
        statusClientService.deleteStatusClient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, String>> getStatusClientNames() {
        return ResponseEntity.ok(statusClientService.getStatusClientNames());
    }

    @GetMapping("/ids")
    public ResponseEntity<List<StatusClientDTO>> findByNameContaining(@RequestParam String query) {
        return ResponseEntity.ok(statusClientService.findByNameContaining(query).stream().map(
                statusClientMapper::statusClientToStatusClientDTO).toList());
    }
}
