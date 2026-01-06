package org.example.purchaseservice.services.product;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.ProductException;
import org.example.purchaseservice.exceptions.ProductNotFoundException;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.services.impl.IProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product with ID %d not found";
    private static final String ALL_USAGE_FILTER = "all";

    private final ProductRepository productRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"products"}, allEntries = true)
    public Product createProduct(@NonNull Product product) {
        log.info("Creating new product: name={}", product.getName());
        
        validateProduct(product);
        String normalizedName = normalizeProductName(product.getName());
        validateProductNameUniqueness(normalizedName, null);
        
        product.setName(normalizedName);
        Product saved = productRepository.save(product);
        log.info("Product created: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products"}, allEntries = true)
    public Product updateProduct(@NonNull Long id, @NonNull Product product) {
        log.info("Updating product: id={}, name={}", id, product.getName());
        
        Product existingProduct = getProductByIdOrThrow(id);
        validateProduct(product);
        
        String normalizedName = normalizeProductName(product.getName());
        if (!normalizedName.equalsIgnoreCase(existingProduct.getName())) {
            validateProductNameUniqueness(normalizedName, id);
        }
        
        existingProduct.setName(normalizedName);
        existingProduct.setUsage(product.getUsage());
        
        Product saved = productRepository.save(existingProduct);
        log.info("Product updated: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products"}, allEntries = true)
    public void deleteProduct(@NonNull Long id) {
        log.info("Deleting product: id={}", id);
        
        Product product = getProductByIdOrThrow(id);
        productRepository.delete(product);
        
        log.info("Product deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(@NonNull Long id) {
        return getProductByIdOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#name")
    public List<Product> findProductsByName(@NonNull String name) {
        String normalizedName = normalizeProductName(name);
        if (normalizedName.isEmpty()) {
            throw new ProductException("INVALID_NAME", "Product name cannot be empty");
        }
        
        log.debug("Searching products by name: name={}", normalizedName);
        return productRepository.findByNameContainingIgnoreCase(normalizedName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#usage")
    public List<Product> getAllProducts(@NonNull String usage) {
        String normalizedUsage = usage.trim();
        if (normalizedUsage.isEmpty()) {
            throw new ProductException("INVALID_USAGE", "Usage filter cannot be empty");
        }

        if (ALL_USAGE_FILTER.equalsIgnoreCase(normalizedUsage)) {
            log.debug("Getting all products");
            Iterable<Product> products = productRepository.findAll();
            return StreamSupport.stream(products.spliterator(), false).toList();
        }

        ProductUsage usageFilter;
        try {
            usageFilter = ProductUsage.valueOf(normalizedUsage.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ProductException("ILLEGAL_USAGE", String.format("Invalid product usage filter: %s", normalizedUsage));
        }

        log.debug("Getting products by usage: usage={}", usageFilter);
        return productRepository.findByUsage(usageFilter);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#usage")
    public List<Product> findProductsByUsage(@NonNull ProductUsage usage) {
        log.debug("Finding products by usage: usage={}", usage);
        return productRepository.findByUsage(usage);
    }

    private Product getProductByIdOrThrow(@NonNull Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format(PRODUCT_NOT_FOUND_MESSAGE, id)));
    }
    
    private void validateProduct(@NonNull Product product) {
        if (product.getName() == null || normalizeProductName(product.getName()).isEmpty()) {
            throw new ProductException("INVALID_PRODUCT_NAME", "Product name is required");
        }
        if (product.getUsage() == null) {
            throw new ProductException("INVALID_PRODUCT_USAGE", "Product usage is required");
        }
    }
    
    private String normalizeProductName(@NonNull String name) {
        return name.trim();
    }
    
    private void validateProductNameUniqueness(@NonNull String name, Long excludeId) {
        String normalizedName = normalizeProductName(name);
        boolean exists;
        
        if (excludeId != null) {
            exists = productRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, excludeId);
        } else {
            exists = productRepository.existsByNameIgnoreCase(normalizedName);
        }
        
        if (exists) {
            throw new ProductException("PRODUCT_NAME_ALREADY_EXISTS",
                    String.format("Product with name '%s' already exists", normalizedName));
        }
    }

}
