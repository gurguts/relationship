package org.example.purchaseservice.services.product;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.ProductException;
import org.example.purchaseservice.exceptions.ProductNotFoundException;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.services.impl.IProductService;
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
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = getProductById(id);
        existingProduct.setName(product.getName());
        existingProduct.setUsage(product.getUsage());
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product with ID %d not found", id)));
    }

    @Override
    public List<Product> findProductsByName(String name) {
        return StreamSupport.stream(productRepository.findAll().spliterator(), false)
                .filter(product -> product.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
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

        return StreamSupport.stream(productRepository.findAll().spliterator(), false)
                .filter(product -> matchesUsage(product.getUsage(), usageFilter))
                .collect(Collectors.toList());
    }

    private boolean matchesUsage(ProductUsage productUsage, ProductUsage filter) {
        return switch (filter) {
            case SALE_ONLY -> productUsage == ProductUsage.SALE_ONLY || productUsage == ProductUsage.BOTH;
            case PURCHASE_ONLY -> productUsage == ProductUsage.PURCHASE_ONLY || productUsage == ProductUsage.BOTH;
            case BOTH -> productUsage == ProductUsage.BOTH;
        };
    }

    @Override
    public List<Product> findProductsByUsage(ProductUsage usage) {
        return StreamSupport.stream(productRepository.findAll().spliterator(), false)
                .filter(product -> product.getUsage() == usage)
                .collect(Collectors.toList());
    }

}
