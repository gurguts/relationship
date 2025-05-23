package org.example.routeservice.mappers;

import org.example.routeservice.models.Route;
import org.example.routeservice.models.dto.RouteCreateDTO;
import org.example.routeservice.models.dto.RouteDTO;
import org.example.routeservice.models.dto.RouteUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {
    public Route routeDTOToRoute(RouteDTO routeDTO) {
        Route route = new Route();
        route.setId(routeDTO.getId());
        route.setName(routeDTO.getName());
        return route;
    }

    public RouteDTO routeToRouteDTO(Route route) {
        RouteDTO routeDTO = new RouteDTO();
        routeDTO.setId(route.getId());
        routeDTO.setName(route.getName());
        return routeDTO;
    }

    public Route routeCreateDTOToRoute(RouteCreateDTO dto) {
        Route route = new Route();
        route.setName(dto.getName());
        return route;
    }

    public Route routeUpdateDTOToRoute(RouteUpdateDTO dto) {
        Route route = new Route();
        route.setName(dto.getName());
        return route;
    }
}
