package org.example.productservice.services.impl;

import org.example.productservice.models.Product;
import org.example.productservice.models.ProductUsage;

import java.util.List;

public interface IProductService {
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    Product getProductById(Long id);
    List<Product> findProductsByName(String name);
    List<Product> getAllProducts(String usage);
    List<Product> findProductsByUsage(ProductUsage usage);
    boolean existsById(Long id);
}
