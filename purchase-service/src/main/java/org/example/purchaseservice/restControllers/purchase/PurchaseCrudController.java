package org.example.purchaseservice.restControllers.purchase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Validated
public class PurchaseCrudController {
    private final IPurchaseCrudService purchaseCrudService;
    private final PurchaseMapper purchaseMapper;
    private final org.example.purchaseservice.services.purchase.PurchaseCrudService purchaseCrudServiceImpl;

    @PreAuthorize("hasAuthority('purchase:create')")
    @PostMapping
    public ResponseEntity<PurchaseDTO> createPurchase(
            @Valid @RequestBody PurchaseCreateDTO purchaseCreateDTO) {
        Purchase purchase = purchaseMapper.purchaseCreateDTOToPurchase(purchaseCreateDTO);
        Purchase createdPurchase = purchaseCrudService.createPurchase(purchase);
        PurchaseDTO createdPurchaseDto = purchaseMapper.toDto(createdPurchase);

        createdPurchaseDto.setIsReceived(false);
        log.info("Purchase created with ID: {}", createdPurchaseDto.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdPurchaseDto.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdPurchaseDto);
    }

    @PreAuthorize("hasAuthority('purchase:edit')")
    @PatchMapping("/{id}")
    public ResponseEntity<PurchaseDTO> updatePurchase(@PathVariable Long id,
                                                      @Valid @RequestBody PurchaseUpdateDTO purchaseDto) {
        Purchase purchase = purchaseMapper.purchaseUpdateDTOToPurchase(purchaseDto);
        Purchase updatedPurchase = purchaseCrudService.updatePurchase(id, purchase);
        PurchaseDTO updatedPurchaseDto = purchaseMapper.toDto(updatedPurchase);

        purchaseCrudServiceImpl.enrichPurchaseDTOWithReceivedStatus(updatedPurchaseDto, updatedPurchase);
        log.info("Purchase updated with ID: {}", updatedPurchaseDto.getId());
        return ResponseEntity.ok(updatedPurchaseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDTO> getPurchaseById(@PathVariable Long id) {
        Purchase purchase = purchaseCrudService.findPurchaseById(id);
        PurchaseDTO purchaseDto = purchaseMapper.toDto(purchase);

        purchaseCrudServiceImpl.enrichPurchaseDTOWithReceivedStatus(purchaseDto, purchase);
        log.info("Purchase fetched with ID: {}", purchaseDto.getId());
        return ResponseEntity.ok(purchaseDto);
    }

    @PreAuthorize("hasAuthority('purchase:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        purchaseCrudService.deletePurchase(id);
        log.info("Purchase deleted with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
