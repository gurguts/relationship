package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.ProductUsage;

import java.util.List;

public interface IProductService {
    Product createProduct(@NonNull Product product);

    Product updateProduct(@NonNull Long id, @NonNull Product product);

    void deleteProduct(@NonNull Long id);

    Product getProductById(@NonNull Long id);

    List<Product> findProductsByName(@NonNull String name);

    List<Product> getAllProducts(@NonNull String usage);

    List<Product> findProductsByUsage(@NonNull ProductUsage usage);
}
