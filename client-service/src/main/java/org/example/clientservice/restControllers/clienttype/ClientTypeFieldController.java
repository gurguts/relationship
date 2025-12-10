package org.example.clientservice.restControllers.clienttype;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.clienttype.ClientTypeFieldMapper;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;
import org.example.clientservice.services.clienttype.ClientTypeService;
import org.example.clientservice.services.clienttype.StaticFieldsHelper;
import org.example.clientservice.services.impl.IClientTypeFieldService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/client-type")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ClientTypeFieldController {
    private final IClientTypeFieldService fieldService;
    private final ClientTypeService clientTypeService;

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping("/{clientTypeId}/field")
    public ResponseEntity<ClientTypeFieldDTO> createField(
            @PathVariable Long clientTypeId,
            @RequestBody @Valid ClientTypeFieldCreateDTO dto) {
        ClientTypeField field = fieldService.createField(clientTypeId, dto);
        ClientTypeFieldDTO response = ClientTypeFieldMapper.toDTO(field);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/../field/{id}")
                .buildAndExpand(field.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientTypeId}/field")
    public ResponseEntity<List<ClientTypeFieldDTO>> getFieldsByClientTypeId(@PathVariable Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clientTypeId}/field/visible")
    public ResponseEntity<List<ClientTypeFieldDTO>> getVisibleFields(@PathVariable Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getVisibleFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());

        try {
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
            StaticFieldsConfig staticConfig = StaticFieldsHelper.parseStaticFieldsConfig(clientType);
            if (staticConfig != null) {
                List<ClientTypeFieldDTO> staticFields = StaticFieldsHelper.createStaticFieldDTOs(staticConfig);
                response.addAll(staticFields);

                response.sort(Comparator.comparingInt(a -> a.getDisplayOrder() != null ? a.getDisplayOrder() : 999));
            }
        } catch (Exception e) {
            log.warn("Failed to add static fields for client type {}: {}", clientTypeId, e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clientTypeId}/field/searchable")
    public ResponseEntity<List<ClientTypeFieldDTO>> getSearchableFields(@PathVariable Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getSearchableFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clientTypeId}/field/filterable")
    public ResponseEntity<List<ClientTypeFieldDTO>> getFilterableFields(@PathVariable Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getFilterableFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:create')")
    @GetMapping("/{clientTypeId}/field/visible-in-create")
    public ResponseEntity<List<ClientTypeFieldDTO>> getVisibleInCreateFields(@PathVariable Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getVisibleInCreateFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/field/{fieldId}")
    public ResponseEntity<ClientTypeFieldDTO> getFieldById(@PathVariable Long fieldId) {
        ClientTypeField field = fieldService.getFieldById(fieldId);
        ClientTypeFieldDTO response = ClientTypeFieldMapper.toDTO(field);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PutMapping("/field/{fieldId}")
    public ResponseEntity<ClientTypeFieldDTO> updateField(
            @PathVariable Long fieldId,
            @RequestBody @Valid ClientTypeFieldUpdateDTO dto) {
        ClientTypeField field = fieldService.updateField(fieldId, dto);
        ClientTypeFieldDTO response = ClientTypeFieldMapper.toDTO(field);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @DeleteMapping("/field/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        fieldService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping("/{clientTypeId}/field/reorder")
    public ResponseEntity<Void> reorderFields(
            @PathVariable Long clientTypeId,
            @RequestBody @Valid FieldReorderDTO dto) {
        fieldService.reorderFields(clientTypeId, dto);
        return ResponseEntity.noContent().build();
    }
}

