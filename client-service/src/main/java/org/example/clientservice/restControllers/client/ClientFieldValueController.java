package org.example.clientservice.restControllers.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.clientservice.repositories.clienttype.ClientFieldValueRepository;
import org.example.clientservice.mappers.clienttype.ClientFieldValueMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Slf4j
public class ClientFieldValueController {
    private final ClientFieldValueRepository fieldValueRepository;
    private final ClientFieldValueMapper fieldValueMapper;

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping("/{clientId}/field-values")
    public ResponseEntity<List<ClientFieldValueDTO>> getClientFieldValues(@PathVariable Long clientId) {
        List<ClientFieldValueDTO> fieldValues = fieldValueRepository.findByClientIdOrderByDisplayOrderAsc(clientId)
                .stream()
                .map(fieldValueMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fieldValues);
    }
}

