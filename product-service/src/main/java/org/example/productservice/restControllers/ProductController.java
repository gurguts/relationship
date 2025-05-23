package org.example.productservice.restControllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.mappers.ProductMapper;
import org.example.productservice.models.Product;
import org.example.productservice.models.ProductUsage;
import org.example.productservice.models.dto.ProductCreateDTO;
import org.example.productservice.models.dto.ProductDTO;
import org.example.productservice.models.dto.ProductUpdateDTO;
import org.example.productservice.services.impl.IProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final IProductService productService;
    private final ProductMapper productMapper;

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductCreateDTO productCreateDTO) {
        Product product = productMapper.productCreateDTOToProduct(productCreateDTO);
        Product createdProduct = productService.createProduct(product);
        ProductDTO createdProductDTO = productMapper.toDto(createdProduct);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProductDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdProductDTO);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO productUpdateDTO) {
        Product product = productMapper.productUpdateDTOToProduct(productUpdateDTO);
        Product updatedProduct = productService.updateProduct(id, product);
        ProductDTO updatedProductDTO = productMapper.toDto(updatedProduct);
        return ResponseEntity.ok(updatedProductDTO);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        ProductDTO productDTO = productMapper.toDto(product);
        return ResponseEntity.ok(productDTO);
    }

    @GetMapping("/{id}/name")
    public ResponseEntity<String> getProductNameById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        ProductDTO productDTO = productMapper.toDto(product);
        return ResponseEntity.ok(productDTO.getName());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> findProductsByName(@RequestParam String name) {
        List<Product> products = productService.findProductsByName(name);
        List<ProductDTO> productDTOs = productMapper.toDtoList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts(@RequestParam(required = false, defaultValue = "all") String usage) {
        List<Product> products = productService.getAllProducts(usage);
        List<ProductDTO> productDTOs = productMapper.toDtoList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/by-usage")
    public ResponseEntity<List<ProductDTO>> findProductsByUsage(@RequestParam ProductUsage usage) {
        List<Product> products = productService.findProductsByUsage(usage);
        List<ProductDTO> productDTOs = productMapper.toDtoList(products);
        return ResponseEntity.ok(productDTOs);
    }
}
