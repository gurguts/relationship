package org.example.routeservice.services.impl;

import org.example.routeservice.models.Route;

import java.util.List;
import java.util.Map;

public interface IRouteService {
    Route getRoute(Long id);
    List<Route> getAllRoutes();
    Route createRoute(Route route);
    Route updateRoute(Long id, Route route);
    void deleteRoute(Long id);

    Map<Long, String> getRouteNames();

    List<Route> findByNameContaining(String query);
}
