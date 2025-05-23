package org.example.clientservice.repositories.field;

import org.example.clientservice.models.field.Business;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BusinessRepository extends CrudRepository<Business, Long> {

    List<Business> findByNameContainingIgnoreCase(String name);
}
