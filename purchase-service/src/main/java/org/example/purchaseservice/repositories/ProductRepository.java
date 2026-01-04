package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Long> {
    @NonNull
    List<Product> findByNameContainingIgnoreCase(@NonNull String name);

    @NonNull
    List<Product> findByUsage(@NonNull ProductUsage usage);
}
