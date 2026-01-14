package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.dto.balance.CarrierCreateDTO;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.CarrierUpdateDTO;
import org.example.purchaseservice.mappers.CarrierMapper;
import org.example.purchaseservice.services.impl.ICarrierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/carriers")
@RequiredArgsConstructor
@Validated
public class CarrierController {
    private final ICarrierService carrierService;
    private final CarrierMapper carrierMapper;

    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping
    public ResponseEntity<CarrierDetailsDTO> createCarrier(@RequestBody @Valid @NonNull CarrierCreateDTO dto) {
        Carrier carrier = carrierMapper.carrierCreateDTOToCarrier(dto);
        Carrier created = carrierService.createCarrier(carrier);
        CarrierDetailsDTO detailsDTO = carrierMapper.carrierToCarrierDetailsDTO(created);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(detailsDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/{carrierId}")
    public ResponseEntity<CarrierDetailsDTO> getCarrierDetails(@PathVariable @Positive Long carrierId) {
        Carrier carrier = carrierService.getCarrier(carrierId);
        CarrierDetailsDTO detailsDTO = carrierMapper.carrierToCarrierDetailsDTO(carrier);
        return ResponseEntity.ok(detailsDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<CarrierDetailsDTO>> getAllCarriers() {
        List<Carrier> carriers = carrierService.getAllCarriers();
        List<CarrierDetailsDTO> dtos = carriers.stream()
                .map(carrierMapper::carrierToCarrierDetailsDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/search")
    public ResponseEntity<List<CarrierDetailsDTO>> searchCarriers(@RequestParam @NonNull String companyName) {
        List<Carrier> carriers = carrierService.searchCarriersByCompanyName(companyName);
        List<CarrierDetailsDTO> dtos = carriers.stream()
                .map(carrierMapper::carrierToCarrierDetailsDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PatchMapping("/{carrierId}")
    public ResponseEntity<CarrierDetailsDTO> updateCarrier(
            @PathVariable @Positive Long carrierId,
            @RequestBody @Valid @NonNull CarrierUpdateDTO dto) {
        Carrier updateData = carrierMapper.carrierUpdateDTOToCarrier(dto);
        Carrier updated = carrierService.updateCarrier(carrierId, updateData);
        return ResponseEntity.ok(carrierMapper.carrierToCarrierDetailsDTO(updated));
    }

    @PreAuthorize("hasAuthority('warehouse:delete') or hasAuthority('declarant:delete')")
    @DeleteMapping("/{carrierId}")
    public ResponseEntity<Void> deleteCarrier(@PathVariable @Positive Long carrierId) {
        carrierService.deleteCarrier(carrierId);
        return ResponseEntity.noContent().build();
    }
}
