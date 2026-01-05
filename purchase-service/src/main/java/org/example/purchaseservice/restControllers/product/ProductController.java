package org.example.purchaseservice.restControllers.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.ProductMapper;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;
import org.example.purchaseservice.models.dto.product.ProductCreateDTO;
import org.example.purchaseservice.models.dto.product.ProductDTO;
import org.example.purchaseservice.models.dto.product.ProductUpdateDTO;
import org.example.purchaseservice.services.impl.IProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final IProductService productService;
    private final ProductMapper productMapper;

    @PreAuthorize("hasAuthority('settings_product:create')")
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody @Valid @NonNull ProductCreateDTO productCreateDTO) {
        Product product = productMapper.productCreateDTOToProduct(productCreateDTO);
        Product createdProduct = productService.createProduct(product);
        ProductDTO createdProductDTO = productMapper.toDto(createdProduct);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProductDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdProductDTO);
    }

    @PreAuthorize("hasAuthority('settings_product:create')")
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable @Positive Long id,
            @RequestBody @Valid @NonNull ProductUpdateDTO productUpdateDTO) {
        Product product = productMapper.productUpdateDTOToProduct(productUpdateDTO);
        Product updatedProduct = productService.updateProduct(id, product);
        ProductDTO updatedProductDTO = productMapper.toDto(updatedProduct);
        return ResponseEntity.ok(updatedProductDTO);
    }

    @PreAuthorize("hasAuthority('settings_product:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable @Positive Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable @Positive Long id) {
        Product product = productService.getProductById(id);
        ProductDTO productDTO = productMapper.toDto(product);
        return ResponseEntity.ok(productDTO);
    }

    @GetMapping("/{id}/name")
    public ResponseEntity<String> getProductNameById(@PathVariable @Positive Long id) {
        Product product = productService.getProductById(id);
        ProductDTO productDTO = productMapper.toDto(product);
        return ResponseEntity.ok(productDTO.getName());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> findProductsByName(@RequestParam @NotBlank String name) {
        List<Product> products = productService.findProductsByName(name);
        List<ProductDTO> productDTOs = productMapper.toDtoList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts(
            @RequestParam(required = false, defaultValue = "all") String usage) {
        List<Product> products = productService.getAllProducts(usage);
        List<ProductDTO> productDTOs = productMapper.toDtoList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/by-usage")
    public ResponseEntity<List<ProductDTO>> findProductsByUsage(@RequestParam @NotNull ProductUsage usage) {
        List<Product> products = productService.findProductsByUsage(usage);
        List<ProductDTO> productDTOs = productMapper.toDtoList(products);
        return ResponseEntity.ok(productDTOs);
    }
}
