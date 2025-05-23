package org.example.clientservice.restControllers.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.ProductClient;
import org.example.clientservice.clients.UserClient;
import org.example.clientservice.mappers.field.*;
import org.example.clientservice.models.dto.fields.EntitiesDTO;
import org.example.clientservice.services.field.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Slf4j
public class EntitiesController {
    private final StatusClientService statusService;
    private final RegionService regionService;
    private final SourceService sourceService;
    private final RouteService routeService;
    private final BusinessService businessService;
    private final UserClient userClient;
    private final ProductClient productClient;

    private final StatusClientMapper statusMapper;
    private final RegionMapper regionMapper;
    private final SourceMapper sourceMapper;
    private final RouteMapper routeMapper;
    private final BusinessMapper businessMapper;

    @GetMapping
    public ResponseEntity<EntitiesDTO> getAllEntities() {
        EntitiesDTO entities = new EntitiesDTO();
        entities.setStatuses(statusService.getAllStatusClients().stream()
                .map(statusMapper::statusClientToStatusClientDTO)
                .toList());
        entities.setRegions(regionService.getAllRegions().stream()
                .map(regionMapper::regionToRegionDTO)
                .toList());
        entities.setSources(sourceService.getAllSources().stream()
                .map(sourceMapper::sourceToSourceDTO)
                .toList());
        entities.setRoutes(routeService.getAllRoutes().stream()
                .map(routeMapper::routeToRouteDTO)
                .toList());
        entities.setBusinesses(businessService.getAllBusinesses().stream()
                .map(businessMapper::businessToBusinessDTO)
                .toList());
        entities.setUsers(Objects.requireNonNull(userClient.getAllUsers().getBody()).stream()
                .toList());
        entities.setProducts(Objects.requireNonNull(productClient.getAllProducts().getBody()).stream()
                .toList());

        return ResponseEntity.ok(entities);
    }
}
