package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.example.purchaseservice.models.dto.balance.VehicleTerminalCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleTerminalDTO;
import org.example.purchaseservice.mappers.VehicleTerminalMapper;
import org.example.purchaseservice.services.balance.VehicleTerminalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicle-terminals")
@RequiredArgsConstructor
@Validated
public class VehicleTerminalController {
    
    private final VehicleTerminalService vehicleTerminalService;
    private final VehicleTerminalMapper vehicleTerminalMapper;
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleTerminalDTO> createVehicleTerminal(@RequestBody @Valid @NonNull VehicleTerminalCreateDTO dto) {
        VehicleTerminal terminal = vehicleTerminalMapper.vehicleTerminalCreateDTOToVehicleTerminal(dto);
        VehicleTerminal created = vehicleTerminalService.createVehicleTerminal(terminal);
        VehicleTerminalDTO detailsDTO = vehicleTerminalMapper.vehicleTerminalToVehicleTerminalDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{terminalId}")
    public ResponseEntity<VehicleTerminalDTO> getVehicleTerminalDetails(@PathVariable @Positive Long terminalId) {
        VehicleTerminal terminal = vehicleTerminalService.getVehicleTerminal(terminalId);
        VehicleTerminalDTO detailsDTO = vehicleTerminalMapper.vehicleTerminalToVehicleTerminalDTO(terminal);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleTerminalDTO>> getAllVehicleTerminals() {
        List<VehicleTerminal> terminals = vehicleTerminalService.getAllVehicleTerminals();
        List<VehicleTerminalDTO> dtos = terminals.stream()
                .map(vehicleTerminalMapper::vehicleTerminalToVehicleTerminalDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PatchMapping("/{terminalId}")
    public ResponseEntity<VehicleTerminalDTO> updateVehicleTerminal(
            @PathVariable @Positive Long terminalId,
            @RequestBody @Valid @NonNull VehicleTerminalCreateDTO dto) {
        VehicleTerminal updateData = vehicleTerminalMapper.vehicleTerminalCreateDTOToVehicleTerminal(dto);
        VehicleTerminal updated = vehicleTerminalService.updateVehicleTerminal(terminalId, updateData);
        return ResponseEntity.ok(vehicleTerminalMapper.vehicleTerminalToVehicleTerminalDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:delete')")
    @DeleteMapping("/{terminalId}")
    public ResponseEntity<Void> deleteVehicleTerminal(@PathVariable @Positive Long terminalId) {
        vehicleTerminalService.deleteVehicleTerminal(terminalId);
        return ResponseEntity.noContent().build();
    }
}
