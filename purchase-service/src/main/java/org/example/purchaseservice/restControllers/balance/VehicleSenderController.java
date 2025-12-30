package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.dto.balance.VehicleSenderCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleSenderDTO;
import org.example.purchaseservice.services.balance.VehicleSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicle-senders")
@RequiredArgsConstructor
public class VehicleSenderController {
    
    private final VehicleSenderService vehicleSenderService;
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleSenderDTO> createVehicleSender(@Valid @RequestBody VehicleSenderCreateDTO dto) {
        VehicleSender sender = vehicleSenderService.createVehicleSender(dto);
        VehicleSenderDTO detailsDTO = mapToDTO(sender);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sender.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{senderId}")
    public ResponseEntity<VehicleSenderDTO> getVehicleSenderDetails(@PathVariable Long senderId) {
        VehicleSender sender = vehicleSenderService.getVehicleSender(senderId);
        VehicleSenderDTO detailsDTO = mapToDTO(sender);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleSenderDTO>> getAllVehicleSenders() {
        List<VehicleSender> senders = vehicleSenderService.getAllVehicleSenders();
        List<VehicleSenderDTO> dtos = senders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PutMapping("/{senderId}")
    public ResponseEntity<VehicleSenderDTO> updateVehicleSender(
            @PathVariable Long senderId,
            @Valid @RequestBody VehicleSenderCreateDTO dto) {
        VehicleSender updated = vehicleSenderService.updateVehicleSender(senderId, dto);
        return ResponseEntity.ok(mapToDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:delete')")
    @DeleteMapping("/{senderId}")
    public ResponseEntity<Void> deleteVehicleSender(@PathVariable Long senderId) {
        vehicleSenderService.deleteVehicleSender(senderId);
        return ResponseEntity.noContent().build();
    }
    
    private VehicleSenderDTO mapToDTO(VehicleSender sender) {
        return VehicleSenderDTO.builder()
                .id(sender.getId())
                .name(sender.getName())
                .createdAt(sender.getCreatedAt())
                .updatedAt(sender.getUpdatedAt())
                .build();
    }
}

