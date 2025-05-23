package org.example.saleservice.services;

import lombok.RequiredArgsConstructor;
import org.example.saleservice.exceptions.ProductException;
import org.example.saleservice.models.Product;
import org.example.saleservice.models.ProductUsage;
import org.example.saleservice.repositories.ProductRepository;
import org.example.saleservice.services.impl.IProductService;
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

}
