package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverDTO;
import org.example.purchaseservice.mappers.VehicleReceiverMapper;
import org.example.purchaseservice.services.impl.IVehicleReceiverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-receivers")
@RequiredArgsConstructor
@Validated
public class VehicleReceiverController {
    
    private final IVehicleReceiverService vehicleReceiverService;
    private final VehicleReceiverMapper vehicleReceiverMapper;
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleReceiverDTO> createVehicleReceiver(@RequestBody @Valid @NonNull VehicleReceiverCreateDTO dto) {
        VehicleReceiver receiver = vehicleReceiverMapper.vehicleReceiverCreateDTOToVehicleReceiver(dto);
        VehicleReceiver created = vehicleReceiverService.createVehicleReceiver(receiver);
        VehicleReceiverDTO detailsDTO = vehicleReceiverMapper.vehicleReceiverToVehicleReceiverDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{receiverId}")
    public ResponseEntity<VehicleReceiverDTO> getVehicleReceiverDetails(@PathVariable @Positive Long receiverId) {
        VehicleReceiver receiver = vehicleReceiverService.getVehicleReceiver(receiverId);
        VehicleReceiverDTO detailsDTO = vehicleReceiverMapper.vehicleReceiverToVehicleReceiverDTO(receiver);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleReceiverDTO>> getAllVehicleReceivers() {
        List<VehicleReceiver> receivers = vehicleReceiverService.getAllVehicleReceivers();
        List<VehicleReceiverDTO> dtos = receivers.stream()
                .map(vehicleReceiverMapper::vehicleReceiverToVehicleReceiverDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PatchMapping("/{receiverId}")
    public ResponseEntity<VehicleReceiverDTO> updateVehicleReceiver(
            @PathVariable @Positive Long receiverId,
            @RequestBody @Valid @NonNull VehicleReceiverCreateDTO dto) {
        VehicleReceiver updateData = vehicleReceiverMapper.vehicleReceiverCreateDTOToVehicleReceiver(dto);
        VehicleReceiver updated = vehicleReceiverService.updateVehicleReceiver(receiverId, updateData);
        return ResponseEntity.ok(vehicleReceiverMapper.vehicleReceiverToVehicleReceiverDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:delete')")
    @DeleteMapping("/{receiverId}")
    public ResponseEntity<Void> deleteVehicleReceiver(@PathVariable @Positive Long receiverId) {
        vehicleReceiverService.deleteVehicleReceiver(receiverId);
        return ResponseEntity.noContent().build();
    }
}

