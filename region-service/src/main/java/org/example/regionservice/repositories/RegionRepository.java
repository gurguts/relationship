package org.example.regionservice.repositories;

import org.example.regionservice.models.Region;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends CrudRepository<Region, Long> {
    Optional<Region> findByName(String name);

    List<Region> findByNameContainingIgnoreCase(String name);
}
