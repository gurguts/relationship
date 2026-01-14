package org.example.userservice.restControllers.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.mappers.CounterpartyMapper;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;
import org.example.userservice.models.dto.transaction.CounterpartyCreateDTO;
import org.example.userservice.models.dto.transaction.CounterpartyDTO;
import org.example.userservice.services.impl.ICounterpartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/counterparties")
@RequiredArgsConstructor
@Validated
public class CounterpartyController {
    private final ICounterpartyService counterpartyService;
    private final CounterpartyMapper counterpartyMapper;

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<CounterpartyDTO>> getCounterpartiesByType(
            @PathVariable @NonNull CounterpartyType type) {
        List<Counterparty> counterparties = counterpartyService.getCounterpartiesByType(type);
        List<CounterpartyDTO> dtos = counterparties.stream()
                .map(counterpartyMapper::counterpartyToCounterpartyDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<CounterpartyDTO> getCounterpartyById(
            @PathVariable @Positive @NonNull Long id) {
        Counterparty counterparty = counterpartyService.getCounterpartyById(id);
        return ResponseEntity.ok(counterpartyMapper.counterpartyToCounterpartyDTO(counterparty));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<CounterpartyDTO> createCounterparty(
            @RequestBody @Valid @NonNull CounterpartyCreateDTO dto) {
        Counterparty counterparty = counterpartyMapper.counterpartyCreateDTOToCounterparty(dto);
        Counterparty created = counterpartyService.createCounterparty(counterparty);
        CounterpartyDTO counterpartyDTO = counterpartyMapper.counterpartyToCounterpartyDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(counterpartyDTO.id())
                .toUri();
        return ResponseEntity.status(CREATED).location(location).body(counterpartyDTO);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<CounterpartyDTO> updateCounterparty(
            @PathVariable @Positive @NonNull Long id,
            @RequestBody @Valid @NonNull CounterpartyCreateDTO dto) {
        Counterparty counterparty = counterpartyMapper.counterpartyCreateDTOToCounterparty(dto);
        Counterparty updated = counterpartyService.updateCounterparty(id, counterparty);
        return ResponseEntity.ok(counterpartyMapper.counterpartyToCounterpartyDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteCounterparty(@PathVariable @Positive @NonNull Long id) {
        counterpartyService.deleteCounterparty(id);
        return ResponseEntity.noContent().build();
    }
}
