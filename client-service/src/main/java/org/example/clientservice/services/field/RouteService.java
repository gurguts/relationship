package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.RouteNotFoundException;
import org.example.clientservice.models.field.Route;
import org.example.clientservice.repositories.field.RouteRepository;
import org.example.clientservice.services.impl.IRouteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService implements IRouteService {
    private final RouteRepository routeRepository;

    @Override
    public Route getRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException(String.format("Route not found with id: %d", id)));
    }

    @Override
    public List<Route> getAllRoutes() {
        return (List<Route>) routeRepository.findAll();
    }

    @Override
    public Route createRoute(Route route) {
        return routeRepository.save(route);
    }

    @Override
    public Route updateRoute(Long id, Route route) {
        Route existingRoute = getRoute(id);
        existingRoute.setName(route.getName());
        return routeRepository.save(existingRoute);
    }

    @Override
    public void deleteRoute(Long id) {
        Route route = getRoute(id);
        routeRepository.delete(route);
    }

    @Override
    public Map<Long, String> getRouteNames() {
        List<Route> routes = (List<Route>) routeRepository.findAll();
        return routes.stream()
                .collect(Collectors.toMap(Route::getId, Route::getName));
    }

    @Override
    public List<Route> findByNameContaining(String query) {
        return routeRepository.findByNameContainingIgnoreCase(query);
    }
}
