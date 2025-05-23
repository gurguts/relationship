package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
