package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.dto.balance.VehicleSenderCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleSenderDTO;
import org.example.purchaseservice.mappers.VehicleSenderMapper;
import org.example.purchaseservice.services.impl.IVehicleSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-senders")
@RequiredArgsConstructor
@Validated
public class VehicleSenderController {
    
    private final IVehicleSenderService vehicleSenderService;
    private final VehicleSenderMapper vehicleSenderMapper;
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleSenderDTO> createVehicleSender(@RequestBody @Valid @NonNull VehicleSenderCreateDTO dto) {
        VehicleSender sender = vehicleSenderMapper.vehicleSenderCreateDTOToVehicleSender(dto);
        VehicleSender created = vehicleSenderService.createVehicleSender(sender);
        VehicleSenderDTO detailsDTO = vehicleSenderMapper.vehicleSenderToVehicleSenderDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{senderId}")
    public ResponseEntity<VehicleSenderDTO> getVehicleSenderDetails(@PathVariable @Positive Long senderId) {
        VehicleSender sender = vehicleSenderService.getVehicleSender(senderId);
        VehicleSenderDTO detailsDTO = vehicleSenderMapper.vehicleSenderToVehicleSenderDTO(sender);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleSenderDTO>> getAllVehicleSenders() {
        List<VehicleSender> senders = vehicleSenderService.getAllVehicleSenders();
        List<VehicleSenderDTO> dtos = senders.stream()
                .map(vehicleSenderMapper::vehicleSenderToVehicleSenderDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PatchMapping("/{senderId}")
    public ResponseEntity<VehicleSenderDTO> updateVehicleSender(
            @PathVariable @Positive Long senderId,
            @RequestBody @Valid @NonNull VehicleSenderCreateDTO dto) {
        VehicleSender updateData = vehicleSenderMapper.vehicleSenderCreateDTOToVehicleSender(dto);
        VehicleSender updated = vehicleSenderService.updateVehicleSender(senderId, updateData);
        return ResponseEntity.ok(vehicleSenderMapper.vehicleSenderToVehicleSenderDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:delete')")
    @DeleteMapping("/{senderId}")
    public ResponseEntity<Void> deleteVehicleSender(@PathVariable @Positive Long senderId) {
        vehicleSenderService.deleteVehicleSender(senderId);
        return ResponseEntity.noContent().build();
    }
}
