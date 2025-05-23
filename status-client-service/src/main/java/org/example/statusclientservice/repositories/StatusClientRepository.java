package org.example.statusclientservice.repositories;

import org.example.statusclientservice.models.StatusClient;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StatusClientRepository extends CrudRepository<StatusClient, Long> {
    List<StatusClient> findByNameContainingIgnoreCase(String name);
}
