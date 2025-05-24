package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.RouteNotFoundException;
import org.example.clientservice.models.field.Route;
import org.example.clientservice.repositories.field.RouteRepository;
import org.example.clientservice.services.impl.IRouteService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService implements IRouteService {
    private final RouteRepository routeRepository;

    @Override
    @Cacheable(value = "routes", key = "#id")
    public Route getRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException(String.format("Route not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "routes", key = "'allRoutes'")
    public List<Route> getAllRoutes() {
        return (List<Route>) routeRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"routes", "routeNames", "routeSearch"}, allEntries = true)
    public Route createRoute(Route route) {
        return routeRepository.save(route);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"routes", "routeNames", "routeSearch"}, allEntries = true)
    public Route updateRoute(Long id, Route route) {
        Route existingRoute = findRoute(id);
        existingRoute.setName(route.getName());
        return routeRepository.save(existingRoute);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"routes", "routeNames", "routeSearch"}, allEntries = true)
    public void deleteRoute(Long id) {
        Route route = findRoute(id);
        routeRepository.delete(route);
    }

    @Override
    @Cacheable(value = "routeNames", key = "'routeNames'")
    public Map<Long, String> getRouteNames() {
        List<Route> routes = (List<Route>) routeRepository.findAll();
        return routes.stream()
                .collect(Collectors.toMap(Route::getId, Route::getName));
    }

    @Override
    @Cacheable(value = "routeSearch", key = "#query")
    public List<Route> findByNameContaining(String query) {
        return routeRepository.findByNameContainingIgnoreCase(query);
    }

    private Route findRoute(Long id) {
        return routeRepository.findById(id).orElseThrow(() ->
                new RouteNotFoundException(String.format("Route not found with id: %d", id)));
    }
}
