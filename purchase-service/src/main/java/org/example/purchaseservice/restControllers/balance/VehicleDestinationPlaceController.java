package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationPlaceCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationPlaceDTO;
import org.example.purchaseservice.mappers.VehicleDestinationPlaceMapper;
import org.example.purchaseservice.services.balance.VehicleDestinationPlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicle-destination-places")
@RequiredArgsConstructor
@Validated
public class VehicleDestinationPlaceController {
    
    private final VehicleDestinationPlaceService vehicleDestinationPlaceService;
    private final VehicleDestinationPlaceMapper vehicleDestinationPlaceMapper;
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleDestinationPlaceDTO> createVehicleDestinationPlace(@RequestBody @Valid @NonNull VehicleDestinationPlaceCreateDTO dto) {
        VehicleDestinationPlace place = vehicleDestinationPlaceMapper.vehicleDestinationPlaceCreateDTOToVehicleDestinationPlace(dto);
        VehicleDestinationPlace created = vehicleDestinationPlaceService.createVehicleDestinationPlace(place);
        VehicleDestinationPlaceDTO detailsDTO = vehicleDestinationPlaceMapper.vehicleDestinationPlaceToVehicleDestinationPlaceDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{placeId}")
    public ResponseEntity<VehicleDestinationPlaceDTO> getVehicleDestinationPlaceDetails(@PathVariable @Positive Long placeId) {
        VehicleDestinationPlace place = vehicleDestinationPlaceService.getVehicleDestinationPlace(placeId);
        VehicleDestinationPlaceDTO detailsDTO = vehicleDestinationPlaceMapper.vehicleDestinationPlaceToVehicleDestinationPlaceDTO(place);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleDestinationPlaceDTO>> getAllVehicleDestinationPlaces() {
        List<VehicleDestinationPlace> places = vehicleDestinationPlaceService.getAllVehicleDestinationPlaces();
        List<VehicleDestinationPlaceDTO> dtos = places.stream()
                .map(vehicleDestinationPlaceMapper::vehicleDestinationPlaceToVehicleDestinationPlaceDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PatchMapping("/{placeId}")
    public ResponseEntity<VehicleDestinationPlaceDTO> updateVehicleDestinationPlace(
            @PathVariable @Positive Long placeId,
            @RequestBody @Valid @NonNull VehicleDestinationPlaceCreateDTO dto) {
        VehicleDestinationPlace updateData = vehicleDestinationPlaceMapper.vehicleDestinationPlaceCreateDTOToVehicleDestinationPlace(dto);
        VehicleDestinationPlace updated = vehicleDestinationPlaceService.updateVehicleDestinationPlace(placeId, updateData);
        return ResponseEntity.ok(vehicleDestinationPlaceMapper.vehicleDestinationPlaceToVehicleDestinationPlaceDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:delete')")
    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> deleteVehicleDestinationPlace(@PathVariable @Positive Long placeId) {
        vehicleDestinationPlaceService.deleteVehicleDestinationPlace(placeId);
        return ResponseEntity.noContent().build();
    }
}
