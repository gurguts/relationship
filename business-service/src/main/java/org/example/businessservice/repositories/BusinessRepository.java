package org.example.businessservice.repositories;

import org.example.businessservice.models.Business;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends CrudRepository<Business, Long> {
    Optional<Business> findByName(String name);

    List<Business> findByNameContainingIgnoreCase(String name);
}
