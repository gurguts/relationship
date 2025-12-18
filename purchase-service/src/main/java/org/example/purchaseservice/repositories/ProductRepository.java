package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByUsage(ProductUsage usage);
}
