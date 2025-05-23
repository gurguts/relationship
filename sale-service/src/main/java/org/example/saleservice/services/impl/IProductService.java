package org.example.saleservice.services.impl;

import org.example.saleservice.models.Product;

import java.util.List;

public interface IProductService {
    List<Product> getAllProducts(String usage);
}
