package org.example.clientservice.services.impl;


import org.example.clientservice.models.field.ClientProduct;

import java.util.List;
import java.util.Map;

public interface IClientProductService {
    ClientProduct getClientProduct(Long id);

    List<ClientProduct> getAllClientProducts();

    ClientProduct createClientProduct(ClientProduct clientProduct);

    ClientProduct updateClientProduct(Long id, ClientProduct clientProduct);

    void deleteClientProduct(Long id);

    Map<Long, String> getClientProductNames();

    List<ClientProduct> findByNameContaining(String query);
}
