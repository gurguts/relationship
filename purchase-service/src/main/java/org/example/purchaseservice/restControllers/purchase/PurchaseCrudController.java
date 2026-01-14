package org.example.purchaseservice.restControllers.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.mappers.PurchaseMapper;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.PurchaseCreateDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseUpdateDTO;
import org.example.purchaseservice.services.impl.IPurchaseCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
@Validated
public class PurchaseCrudController {
    private final IPurchaseCrudService purchaseCrudService;
    private final PurchaseMapper purchaseMapper;

    @PreAuthorize("hasAuthority('purchase:create')")
    @PostMapping
    public ResponseEntity<PurchaseDTO> createPurchase(
            @RequestBody @Valid @NonNull PurchaseCreateDTO purchaseCreateDTO) {
        Purchase purchase = purchaseMapper.purchaseCreateDTOToPurchase(purchaseCreateDTO);
        Purchase createdPurchase = purchaseCrudService.createPurchase(purchase);
        PurchaseDTO createdPurchaseDto = purchaseMapper.toDtoForCreate(createdPurchase);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdPurchaseDto.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdPurchaseDto);
    }

    @PreAuthorize("hasAuthority('purchase:edit')")
    @PatchMapping("/{id}")
    public ResponseEntity<PurchaseDTO> updatePurchase(
            @PathVariable @Positive Long id,
            @RequestBody @Valid @NonNull PurchaseUpdateDTO purchaseDto) {
        Purchase purchase = purchaseMapper.purchaseUpdateDTOToPurchase(purchaseDto);
        Purchase updatedPurchase = purchaseCrudService.updatePurchase(id, purchase);
        PurchaseDTO updatedPurchaseDto = purchaseMapper.toDto(updatedPurchase);
        purchaseCrudService.enrichPurchaseDTOWithReceivedStatus(updatedPurchaseDto, updatedPurchase);
        return ResponseEntity.ok(updatedPurchaseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDTO> getPurchaseById(@PathVariable @Positive Long id) {
        Purchase purchase = purchaseCrudService.findPurchaseById(id);
        PurchaseDTO purchaseDto = purchaseMapper.toDto(purchase);
        purchaseCrudService.enrichPurchaseDTOWithReceivedStatus(purchaseDto, purchase);
        return ResponseEntity.ok(purchaseDto);
    }

    @PreAuthorize("hasAuthority('purchase:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable @Positive Long id) {
        purchaseCrudService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }
}
