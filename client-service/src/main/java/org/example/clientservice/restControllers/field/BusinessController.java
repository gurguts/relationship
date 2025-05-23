package org.example.clientservice.restControllers.field;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.field.BusinessMapper;
import org.example.clientservice.models.dto.fields.BusinessCreateDTO;
import org.example.clientservice.models.dto.fields.BusinessDTO;
import org.example.clientservice.models.dto.fields.BusinessUpdateDTO;
import org.example.clientservice.models.field.Business;
import org.example.clientservice.services.impl.IBusinessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@Slf4j
public class BusinessController {
    private final BusinessMapper businessMapper;
    private final IBusinessService businessService;

    @GetMapping("/{id}")
    public ResponseEntity<BusinessDTO> getBusiness(@PathVariable Long id) {
        BusinessDTO businessDTO = businessMapper.businessToBusinessDTO(businessService.getBusiness(id));
        return ResponseEntity.ok(businessDTO);
    }

    @GetMapping
    public ResponseEntity<List<BusinessDTO>> getBusinesses() {
        List<Business> businesses = businessService.getAllBusinesses();
        List<BusinessDTO> dtos = businesses.stream()
                .map(businessMapper::businessToBusinessDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<BusinessDTO> createBusiness(@RequestBody @Valid BusinessCreateDTO businessCreateDTO) {
        Business business = businessMapper.businessCreateDTOToBusiness(businessCreateDTO);
        BusinessDTO createdBusiness = businessMapper.businessToBusinessDTO(businessService.createBusiness(business));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBusiness.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdBusiness);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<BusinessDTO> updateBusiness(@PathVariable Long id,
                                                      @RequestBody @Valid BusinessUpdateDTO businessUpdateDTO) {
        Business business = businessMapper.businessUpdateDTOToBusiness(businessUpdateDTO);
        Business updatedBusiness = businessService.updateBusiness(id, business);
        return ResponseEntity.ok(businessMapper.businessToBusinessDTO(updatedBusiness));
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        businessService.deleteBusiness(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, String>> getBusinessNames() {
        return ResponseEntity.ok(businessService.getBusinessNames());
    }

    @GetMapping("/ids")
    public ResponseEntity<List<BusinessDTO>> findByNameContaining(@RequestParam String query) {
        return ResponseEntity.ok(businessService.findByNameContaining(query).stream().map(
                businessMapper::businessToBusinessDTO).toList());
    }
}
