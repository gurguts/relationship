package org.example.purchaseservice.services.product;

import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"products"}, allEntries = true)
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products"}, allEntries = true)
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product with ID %d not found", id)));
        existingProduct.setName(product.getName());
        existingProduct.setUsage(product.getUsage());
        return productRepository.save(existingProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products"}, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product with ID %d not found", id)));
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product with ID %d not found", id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#name")
    public List<Product> findProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#usage")
    public List<Product> getAllProducts(String usage) {

        if ("all".equalsIgnoreCase(usage)) {
            return StreamSupport.stream(productRepository.findAll().spliterator(), false)
                    .collect(Collectors.toList());
        }

        ProductUsage usageFilter;
        try {
            usageFilter = ProductUsage.valueOf(usage.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ProductException("ILLEGAL_USAGE", String.format("Invalid product usage filter: %s", usage));
        }

        return productRepository.findByUsage(usageFilter);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#usage")
    public List<Product> findProductsByUsage(ProductUsage usage) {
        return productRepository.findByUsage(usage);
    }

}
