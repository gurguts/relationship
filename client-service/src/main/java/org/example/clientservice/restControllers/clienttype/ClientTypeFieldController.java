package org.example.clientservice.restControllers.clienttype;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.mappers.clienttype.ClientTypeFieldMapper;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldUpdateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldsAllDTO;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.models.dto.clienttype.FieldReorderDTO;
import org.example.clientservice.services.impl.IClientTypeFieldService;
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
@Validated
public class ClientTypeFieldController {
    private final IClientTypeFieldService fieldService;

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping("/{clientTypeId}/field")
    public ResponseEntity<ClientTypeFieldDTO> createField(
            @PathVariable @Positive Long clientTypeId,
            @RequestBody @Valid @NonNull ClientTypeFieldCreateDTO dto) {
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
    public ResponseEntity<List<ClientTypeFieldDTO>> getFieldsByClientTypeId(@PathVariable @Positive Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientTypeId}/field/visible")
    public ResponseEntity<List<ClientTypeFieldDTO>> getVisibleFields(@PathVariable @Positive Long clientTypeId) {
        List<ClientTypeFieldDTO> response = fieldService.getVisibleFieldsWithStatic(clientTypeId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientTypeId}/field/searchable")
    public ResponseEntity<List<ClientTypeFieldDTO>> getSearchableFields(@PathVariable @Positive Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getSearchableFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientTypeId}/field/filterable")
    public ResponseEntity<List<ClientTypeFieldDTO>> getFilterableFields(@PathVariable @Positive Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getFilterableFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:create')")
    @GetMapping("/{clientTypeId}/field/visible-in-create")
    public ResponseEntity<List<ClientTypeFieldDTO>> getVisibleInCreateFields(@PathVariable @Positive Long clientTypeId) {
        List<ClientTypeField> fields = fieldService.getVisibleInCreateFieldsByClientTypeId(clientTypeId);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/field/{fieldId}")
    public ResponseEntity<ClientTypeFieldDTO> getFieldById(@PathVariable @Positive Long fieldId) {
        ClientTypeField field = fieldService.getFieldById(fieldId);
        ClientTypeFieldDTO response = ClientTypeFieldMapper.toDTO(field);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/field/ids")
    public ResponseEntity<List<ClientTypeFieldDTO>> getFieldsByIds(@RequestBody @Valid @NonNull FieldIdsRequest request) {
        List<ClientTypeField> fields = fieldService.getFieldsByIds(request);
        List<ClientTypeFieldDTO> response = fields.stream()
                .map(ClientTypeFieldMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PutMapping("/field/{fieldId}")
    public ResponseEntity<ClientTypeFieldDTO> updateField(
            @PathVariable @Positive Long fieldId,
            @RequestBody @Valid @NonNull ClientTypeFieldUpdateDTO dto) {
        ClientTypeField field = fieldService.updateField(fieldId, dto);
        ClientTypeFieldDTO response = ClientTypeFieldMapper.toDTO(field);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @DeleteMapping("/field/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable @Positive Long fieldId) {
        fieldService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping("/{clientTypeId}/field/reorder")
    public ResponseEntity<Void> reorderFields(
            @PathVariable @Positive Long clientTypeId,
            @RequestBody @Valid @NonNull FieldReorderDTO dto) {
        fieldService.reorderFields(clientTypeId, dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientTypeId}/fields/all")
    public ResponseEntity<ClientTypeFieldsAllDTO> getAllFields(@PathVariable @Positive Long clientTypeId) {
        ClientTypeFieldsAllDTO response = fieldService.getAllFieldsByClientTypeId(clientTypeId);
        return ResponseEntity.ok(response);
    }
}

