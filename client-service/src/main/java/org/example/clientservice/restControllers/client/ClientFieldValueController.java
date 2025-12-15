package org.example.clientservice.restControllers.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.clientservice.repositories.clienttype.ClientFieldValueRepository;
import org.example.clientservice.mappers.clienttype.ClientFieldValueMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @PreAuthorize("hasAuthority('client:view')")
    @PostMapping("/field-values/batch")
    public ResponseEntity<Map<Long, List<ClientFieldValueDTO>>> getClientFieldValuesBatch(@RequestBody List<Long> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
        
        List<ClientFieldValue> allFieldValues = fieldValueRepository.findByClientIdInOrderByClientIdAscDisplayOrderAsc(clientIds);
        
        Map<Long, List<ClientFieldValueDTO>> result = allFieldValues.stream()
                .collect(Collectors.groupingBy(
                        fv -> fv.getClient().getId(),
                        Collectors.mapping(
                                fieldValueMapper::toDTO,
                                Collectors.toList()
                        )
                ));
        
        return ResponseEntity.ok(result);
    }
}

