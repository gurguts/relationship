package org.example.clientservice.restControllers.field;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.field.ClientProductMapper;
import org.example.clientservice.models.dto.fields.*;
import org.example.clientservice.models.field.ClientProduct;
import org.example.clientservice.services.impl.IClientProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clientProduct")
@RequiredArgsConstructor
@Slf4j
public class ClientProductController {
    private final IClientProductService clientProductService;
    private final ClientProductMapper clientProductMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ClientProductDTO> getClientProduct(@PathVariable Long id) {
        ClientProductDTO clientProductDTO = clientProductMapper.clientProductToClientProductDTO(clientProductService.getClientProduct(id));
        return ResponseEntity.ok(clientProductDTO);
    }

    @GetMapping
    public ResponseEntity<List<ClientProductDTO>> getClientProducts() {
        List<ClientProduct> clientProducts = clientProductService.getAllClientProducts();
        List<ClientProductDTO> dtos = clientProducts.stream()
                .map(clientProductMapper::clientProductToClientProductDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('settings_client:create')")
    @PostMapping
    public ResponseEntity<ClientProductDTO> createClientProduct(@RequestBody @Valid ClientProductCreateDTO clientProductCreateDTO) {
        ClientProduct clientProduct = clientProductMapper.clientProductCreateDTOToClientProduct(clientProductCreateDTO);
        ClientProductDTO createdClientProduct = clientProductMapper.clientProductToClientProductDTO(clientProductService.createClientProduct(clientProduct));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClientProduct.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdClientProduct);
    }

    @PreAuthorize("hasAuthority('settings_client:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ClientProductDTO> updateClientProduct(@PathVariable Long id,
                                                  @RequestBody @Valid ClientProductUpdateDTO clientProductUpdateDTO) {
        ClientProduct clientProduct = clientProductMapper.clientProductUpdateDTOToClientProduct(clientProductUpdateDTO);
        ClientProduct updateClientProduct = clientProductService.updateClientProduct(id, clientProduct);
        return ResponseEntity.ok(clientProductMapper.clientProductToClientProductDTO(updateClientProduct));
    }

    @PreAuthorize("hasAuthority('settings_client:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientProduct(@PathVariable Long id) {
        clientProductService.deleteClientProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, String>> getClientProductNames() {
        return ResponseEntity.ok(clientProductService.getClientProductNames());
    }

    @GetMapping("/ids")
    public ResponseEntity<List<ClientProductDTO>> findByNameContaining(@RequestParam String query) {
        return ResponseEntity.ok(clientProductService.findByNameContaining(query).stream().map(
                clientProductMapper::clientProductToClientProductDTO).toList());
    }
}
