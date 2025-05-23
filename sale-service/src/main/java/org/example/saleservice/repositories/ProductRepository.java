package org.example.saleservice.repositories;

import org.example.saleservice.models.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
