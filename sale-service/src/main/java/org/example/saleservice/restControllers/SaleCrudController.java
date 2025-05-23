package org.example.saleservice.restControllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saleservice.mappers.SaleMapper;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.fields.SaleCreateDTO;
import org.example.saleservice.models.dto.fields.SaleDTO;
import org.example.saleservice.models.dto.fields.SaleUpdateDTO;
import org.example.saleservice.services.impl.ISaleCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/sale")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SaleCrudController {
    private final ISaleCrudService saleCrudService;
    private final SaleMapper saleMapper;

    @PreAuthorize("hasAuthority('sale:create')")
    @PostMapping
    public ResponseEntity<SaleDTO> createSale(@Valid @RequestBody SaleCreateDTO saleCreateDTO) {
        Sale sale = saleMapper.saleCreateDTOToSale(saleCreateDTO);
        Sale createdPurchase = saleCrudService.createSale(sale);
        SaleDTO createdSaleDTO = saleMapper.toDto(createdPurchase);
        log.info("Sale created with ID: {}", createdSaleDTO.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSaleDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdSaleDTO);
    }

    @PreAuthorize("hasAuthority('sale:edit')")
    @PatchMapping("/{id}")
    public ResponseEntity<SaleDTO> updateSale(@PathVariable Long id, @Valid @RequestBody SaleUpdateDTO saleUpdateDTO) {
        Sale sale = saleMapper.saleUpdateDTOToSale(saleUpdateDTO);
        Sale updatedPurchase = saleCrudService.updateSale(id, sale);
        SaleDTO updatedSaleDTO = saleMapper.toDto(updatedPurchase);
        log.info("Sale updated with ID: {}", updatedSaleDTO.getId());
        return ResponseEntity.ok(updatedSaleDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDTO> getSaleById(@PathVariable Long id) {
        Sale sale = saleCrudService.findSaleById(id);
        SaleDTO saleDTO = saleMapper.toDto(sale);
        log.info("Sale fetched with ID: {}", saleDTO.getId());
        return ResponseEntity.ok(saleDTO);
    }

    @PreAuthorize("hasAuthority('sale:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleCrudService.deleteSale(id);
        log.info("Sale deleted with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
