package org.example.clientservice.repositories.field;

import org.example.clientservice.models.field.ClientProduct;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ClientProductRepository extends CrudRepository<ClientProduct, Long> {

    List<ClientProduct> findByNameContainingIgnoreCase(String name);
}
