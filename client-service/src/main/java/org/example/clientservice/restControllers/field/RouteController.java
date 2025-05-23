package org.example.clientservice.restControllers.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.field.RouteMapper;
import org.example.clientservice.models.dto.fields.RouteCreateDTO;
import org.example.clientservice.models.dto.fields.RouteDTO;
import org.example.clientservice.models.dto.fields.RouteUpdateDTO;
import org.example.clientservice.models.field.Route;
import org.example.clientservice.services.impl.IRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/route")
@RequiredArgsConstructor
@Slf4j
public class RouteController {
    private final IRouteService routeService;
    private final RouteMapper routeMapper;

    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getRoute(@PathVariable Long id) {
        Route route = routeService.getRoute(id);
        return ResponseEntity.ok(routeMapper.routeToRouteDTO(route));
    }

    @GetMapping
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        List<Route> routes = routeService.getAllRoutes();
        List<RouteDTO> dtos = routes.stream()
                .map(routeMapper::routeToRouteDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<RouteDTO> createRoute(@RequestBody RouteCreateDTO routeCreateDTO) {
        Route route = routeMapper.routeCreateDTOToRoute(routeCreateDTO);
        RouteDTO createdRoute = routeMapper.routeToRouteDTO(routeService.createRoute(route));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRoute.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdRoute);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<RouteDTO> updateRoute(@PathVariable Long id, @RequestBody RouteUpdateDTO routeUpdateDTO) {
        Route route = routeMapper.routeUpdateDTOToRoute(routeUpdateDTO);
        Route updatedRoute = routeService.updateRoute(id, route);
        return ResponseEntity.ok(routeMapper.routeToRouteDTO(updatedRoute));
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, String>> getRouteNames() {
        return ResponseEntity.ok(routeService.getRouteNames());
    }

    @GetMapping("/ids")
    public ResponseEntity<List<RouteDTO>> findByNameContaining(@RequestParam String query) {
        return ResponseEntity.ok(routeService.findByNameContaining(query).stream().map(
                routeMapper::routeToRouteDTO).toList());
    }
}
