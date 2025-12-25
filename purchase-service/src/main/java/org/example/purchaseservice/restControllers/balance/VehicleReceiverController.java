package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverDTO;
import org.example.purchaseservice.services.balance.VehicleReceiverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicle-receivers")
@RequiredArgsConstructor
public class VehicleReceiverController {
    
    private final VehicleReceiverService vehicleReceiverService;
    
    @PreAuthorize("hasAuthority('settings_finance:create')")
    @PostMapping
    public ResponseEntity<VehicleReceiverDTO> createVehicleReceiver(@Valid @RequestBody VehicleReceiverCreateDTO dto) {
        VehicleReceiver receiver = vehicleReceiverService.createVehicleReceiver(dto);
        VehicleReceiverDTO detailsDTO = mapToDTO(receiver);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(receiver.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{receiverId}")
    public ResponseEntity<VehicleReceiverDTO> getVehicleReceiverDetails(@PathVariable Long receiverId) {
        VehicleReceiver receiver = vehicleReceiverService.getVehicleReceiver(receiverId);
        VehicleReceiverDTO detailsDTO = mapToDTO(receiver);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleReceiverDTO>> getAllVehicleReceivers() {
        List<VehicleReceiver> receivers = vehicleReceiverService.getAllVehicleReceivers();
        List<VehicleReceiverDTO> dtos = receivers.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_finance:create')")
    @PutMapping("/{receiverId}")
    public ResponseEntity<VehicleReceiverDTO> updateVehicleReceiver(
            @PathVariable Long receiverId,
            @Valid @RequestBody VehicleReceiverCreateDTO dto) {
        VehicleReceiver updated = vehicleReceiverService.updateVehicleReceiver(receiverId, dto);
        return ResponseEntity.ok(mapToDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    @DeleteMapping("/{receiverId}")
    public ResponseEntity<Void> deleteVehicleReceiver(@PathVariable Long receiverId) {
        vehicleReceiverService.deleteVehicleReceiver(receiverId);
        return ResponseEntity.noContent().build();
    }
    
    private VehicleReceiverDTO mapToDTO(VehicleReceiver receiver) {
        return VehicleReceiverDTO.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .createdAt(receiver.getCreatedAt())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }
}

