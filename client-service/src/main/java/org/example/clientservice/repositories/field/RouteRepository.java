package org.example.clientservice.repositories.field;

import org.example.clientservice.models.field.Route;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RouteRepository extends CrudRepository<Route, Long> {

    List<Route> findByNameContainingIgnoreCase(String name);
}
