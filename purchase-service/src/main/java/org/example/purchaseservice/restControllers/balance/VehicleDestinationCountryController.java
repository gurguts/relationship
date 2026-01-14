package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationCountryCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationCountryDTO;
import org.example.purchaseservice.mappers.VehicleDestinationCountryMapper;
import org.example.purchaseservice.services.impl.IVehicleDestinationCountryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-destination-countries")
@RequiredArgsConstructor
@Validated
public class VehicleDestinationCountryController {
    
    private final IVehicleDestinationCountryService vehicleDestinationCountryService;
    private final VehicleDestinationCountryMapper vehicleDestinationCountryMapper;
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PostMapping
    public ResponseEntity<VehicleDestinationCountryDTO> createVehicleDestinationCountry(@RequestBody @Valid @NonNull VehicleDestinationCountryCreateDTO dto) {
        VehicleDestinationCountry country = vehicleDestinationCountryMapper.vehicleDestinationCountryCreateDTOToVehicleDestinationCountry(dto);
        VehicleDestinationCountry created = vehicleDestinationCountryService.createVehicleDestinationCountry(country);
        VehicleDestinationCountryDTO detailsDTO = vehicleDestinationCountryMapper.vehicleDestinationCountryToVehicleDestinationCountryDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping("/{countryId}")
    public ResponseEntity<VehicleDestinationCountryDTO> getVehicleDestinationCountryDetails(@PathVariable @Positive Long countryId) {
        VehicleDestinationCountry country = vehicleDestinationCountryService.getVehicleDestinationCountry(countryId);
        VehicleDestinationCountryDTO detailsDTO = vehicleDestinationCountryMapper.vehicleDestinationCountryToVehicleDestinationCountryDTO(country);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<VehicleDestinationCountryDTO>> getAllVehicleDestinationCountries() {
        List<VehicleDestinationCountry> countries = vehicleDestinationCountryService.getAllVehicleDestinationCountries();
        List<VehicleDestinationCountryDTO> dtos = countries.stream()
                .map(vehicleDestinationCountryMapper::vehicleDestinationCountryToVehicleDestinationCountryDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:create')")
    @PatchMapping("/{countryId}")
    public ResponseEntity<VehicleDestinationCountryDTO> updateVehicleDestinationCountry(
            @PathVariable @Positive Long countryId,
            @RequestBody @Valid @NonNull VehicleDestinationCountryCreateDTO dto) {
        VehicleDestinationCountry updateData = vehicleDestinationCountryMapper.vehicleDestinationCountryCreateDTOToVehicleDestinationCountry(dto);
        VehicleDestinationCountry updated = vehicleDestinationCountryService.updateVehicleDestinationCountry(countryId, updateData);
        return ResponseEntity.ok(vehicleDestinationCountryMapper.vehicleDestinationCountryToVehicleDestinationCountryDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('settings_declarant:delete')")
    @DeleteMapping("/{countryId}")
    public ResponseEntity<Void> deleteVehicleDestinationCountry(@PathVariable @Positive Long countryId) {
        vehicleDestinationCountryService.deleteVehicleDestinationCountry(countryId);
        return ResponseEntity.noContent().build();
    }
}
