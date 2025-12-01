package org.example.clientservice.restControllers.clienttype;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.clienttype.ClientTypeMapper;
import org.example.clientservice.models.client.PageResponse;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeUpdateDTO;
import org.example.clientservice.services.impl.IClientTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/client-type")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ClientTypeController {
    private final IClientTypeService clientTypeService;
    private final ClientTypeMapper clientTypeMapper;

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping
    public ResponseEntity<ClientTypeDTO> createClientType(@RequestBody @Valid ClientTypeCreateDTO dto) {
        ClientType clientType = clientTypeService.createClientType(dto);
        ClientTypeDTO response = clientTypeMapper.clientTypeToDTO(clientType);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(clientType.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping
    public ResponseEntity<PageResponse<ClientTypeDTO>> getAllClientTypes(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean activeOnly) {
        Page<ClientType> clientTypes;
        if (Boolean.TRUE.equals(activeOnly)) {
            clientTypes = clientTypeService.getAllActiveClientTypes(pageable);
        } else {
            clientTypes = clientTypeService.getAllClientTypes(pageable);
        }
        Page<ClientTypeDTO> clientTypesPage = clientTypes.map(clientTypeMapper::clientTypeToDTO);
        PageResponse<ClientTypeDTO> response = new PageResponse<>(clientTypesPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ClientTypeDTO>> getAllActiveClientTypes() {
        List<ClientType> clientTypes = clientTypeService.getAllActiveClientTypes();
        List<ClientTypeDTO> response = clientTypes.stream()
                .map(clientTypeMapper::clientTypeToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ClientTypeDTO> getClientTypeById(@PathVariable Long id) {
        ClientType clientType = clientTypeService.getClientTypeByIdWithFields(id);
        ClientTypeDTO response = clientTypeMapper.clientTypeToDTO(clientType);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ClientTypeDTO> updateClientType(
            @PathVariable Long id,
            @RequestBody @Valid ClientTypeUpdateDTO dto) {
        ClientType clientType = clientTypeService.updateClientType(id, dto);
        ClientTypeDTO response = clientTypeMapper.clientTypeToDTO(clientType);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientType(@PathVariable Long id) {
        clientTypeService.deleteClientType(id);
        return ResponseEntity.noContent().build();
    }
}

