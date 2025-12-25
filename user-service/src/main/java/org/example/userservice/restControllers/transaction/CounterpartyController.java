package org.example.userservice.restControllers.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;
import org.example.userservice.models.dto.transaction.CounterpartyCreateDTO;
import org.example.userservice.models.dto.transaction.CounterpartyDTO;
import org.example.userservice.services.transaction.CounterpartyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {
    private final CounterpartyService counterpartyService;

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<CounterpartyDTO>> getCounterpartiesByType(@PathVariable CounterpartyType type) {
        List<Counterparty> counterparties = counterpartyService.getCounterpartiesByType(type);
        List<CounterpartyDTO> dtos = counterparties.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<CounterpartyDTO> getCounterpartyById(@PathVariable Long id) {
        Counterparty counterparty = counterpartyService.getCounterpartyById(id);
        return ResponseEntity.ok(mapToDTO(counterparty));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<CounterpartyDTO> createCounterparty(@RequestBody CounterpartyCreateDTO dto) {
        Counterparty counterparty = new Counterparty();
        counterparty.setType(dto.getType());
        counterparty.setName(dto.getName());
        counterparty.setDescription(dto.getDescription());
        Counterparty created = counterpartyService.createCounterparty(counterparty);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<CounterpartyDTO> updateCounterparty(@PathVariable Long id, @RequestBody CounterpartyCreateDTO dto) {
        Counterparty counterparty = new Counterparty();
        counterparty.setName(dto.getName());
        counterparty.setDescription(dto.getDescription());
        Counterparty updated = counterpartyService.updateCounterparty(id, counterparty);
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteCounterparty(@PathVariable Long id) {
        counterpartyService.deleteCounterparty(id);
        return ResponseEntity.noContent().build();
    }

    private CounterpartyDTO mapToDTO(Counterparty counterparty) {
        return new CounterpartyDTO(
                counterparty.getId(),
                counterparty.getType(),
                counterparty.getName(),
                counterparty.getDescription(),
                counterparty.getCreatedAt(),
                counterparty.getUpdatedAt()
        );
    }
}

