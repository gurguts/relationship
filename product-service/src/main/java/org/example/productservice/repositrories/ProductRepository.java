package org.example.productservice.repositrories;

import org.example.productservice.models.Product;
import org.example.productservice.models.ProductUsage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Long> {
    List<Product> findByUsage(ProductUsage usage);
}
