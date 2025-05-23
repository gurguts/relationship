package org.example.clientservice.repositories.field;

import org.example.clientservice.models.field.StatusClient;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StatusClientRepository extends CrudRepository<StatusClient, Long> {

    List<StatusClient> findByNameContainingIgnoreCase(String name);
}
