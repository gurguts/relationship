package org.example.routeservice.repositories;

import org.example.routeservice.models.Route;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends CrudRepository<Route, Long> {
    Optional<Route> findByName(String name);

    List<Route> findByNameContainingIgnoreCase(String name);
}
