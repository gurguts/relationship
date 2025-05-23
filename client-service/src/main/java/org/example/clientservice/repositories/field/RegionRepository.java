package org.example.clientservice.repositories.field;

import org.example.clientservice.models.field.Region;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RegionRepository extends CrudRepository<Region, Long> {

    List<Region> findByNameContainingIgnoreCase(String name);
}
