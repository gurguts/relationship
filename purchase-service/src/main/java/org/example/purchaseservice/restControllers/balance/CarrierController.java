package org.example.purchaseservice.restControllers.balance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.dto.balance.CarrierCreateDTO;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.CarrierUpdateDTO;
import org.example.purchaseservice.services.balance.CarrierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/carriers")
@RequiredArgsConstructor
public class CarrierController {
    
    private final CarrierService carrierService;
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PostMapping
    public ResponseEntity<CarrierDetailsDTO> createCarrier(@Valid @RequestBody CarrierCreateDTO dto) {
        Carrier carrier = carrierService.createCarrier(dto);
        CarrierDetailsDTO detailsDTO = mapToDetailsDTO(carrier);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(carrier.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/{carrierId}")
    public ResponseEntity<CarrierDetailsDTO> getCarrierDetails(@PathVariable Long carrierId) {
        Carrier carrier = carrierService.getCarrier(carrierId);
        CarrierDetailsDTO detailsDTO = mapToDetailsDTO(carrier);
        return ResponseEntity.ok(detailsDTO);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping
    public ResponseEntity<List<CarrierDetailsDTO>> getAllCarriers() {
        List<Carrier> carriers = carrierService.getAllCarriers();
        List<CarrierDetailsDTO> dtos = carriers.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('warehouse:view') or hasAuthority('declarant:view')")
    @GetMapping("/search")
    public ResponseEntity<List<CarrierDetailsDTO>> searchCarriers(
            @RequestParam String companyName) {
        List<Carrier> carriers = carrierService.searchCarriersByCompanyName(companyName);
        List<CarrierDetailsDTO> dtos = carriers.stream()
                .map(this::mapToDetailsDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('warehouse:create') or hasAuthority('declarant:create')")
    @PutMapping("/{carrierId}")
    public ResponseEntity<CarrierDetailsDTO> updateCarrier(
            @PathVariable Long carrierId,
            @RequestBody CarrierUpdateDTO dto) {
        Carrier updated = carrierService.updateCarrier(carrierId, dto);
        return ResponseEntity.ok(mapToDetailsDTO(updated));
    }
    
    @PreAuthorize("hasAuthority('warehouse:delete') or hasAuthority('declarant:delete')")
    @DeleteMapping("/{carrierId}")
    public ResponseEntity<Void> deleteCarrier(@PathVariable Long carrierId) {
        carrierService.deleteCarrier(carrierId);
        return ResponseEntity.noContent().build();
    }
    
    private CarrierDetailsDTO mapToDetailsDTO(Carrier carrier) {
        return CarrierDetailsDTO.builder()
                .id(carrier.getId())
                .companyName(carrier.getCompanyName())
                .registrationAddress(carrier.getRegistrationAddress())
                .phoneNumber(carrier.getPhoneNumber())
                .code(carrier.getCode())
                .account(carrier.getAccount())
                .createdAt(carrier.getCreatedAt())
                .updatedAt(carrier.getUpdatedAt())
                .build();
    }
}

