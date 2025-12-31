package org.example.clientservice.restControllers.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.dto.client.ClientIdsRequest;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.clientservice.services.impl.IClientFieldValueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Validated
public class ClientFieldValueController {
    private final IClientFieldValueService fieldValueService;

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientId}/field-values")
    public ResponseEntity<List<ClientFieldValueDTO>> getClientFieldValues(
            @PathVariable @Positive Long clientId) {
        List<ClientFieldValueDTO> fieldValues = fieldValueService.getFieldValuesByClientId(clientId);
        return ResponseEntity.ok(fieldValues);
    }

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/field-values/batch")
    public ResponseEntity<Map<Long, List<ClientFieldValueDTO>>> getClientFieldValuesBatch(
            @RequestBody @Valid @NonNull ClientIdsRequest request) {
        Map<Long, List<ClientFieldValueDTO>> result = fieldValueService.getFieldValuesByClientIds(request.clientIds());
        return ResponseEntity.ok(result);
    }
}

