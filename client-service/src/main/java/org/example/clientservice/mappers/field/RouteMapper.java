package org.example.clientservice.mappers.field;

import org.example.clientservice.models.dto.fields.RouteCreateDTO;
import org.example.clientservice.models.dto.fields.RouteDTO;
import org.example.clientservice.models.dto.fields.RouteUpdateDTO;
import org.example.clientservice.models.field.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteDTO routeToRouteDTO(Route route) {
        RouteDTO routeDTO = new RouteDTO();
        routeDTO.setId(route.getId());
        routeDTO.setName(route.getName());
        return routeDTO;
    }

    public Route routeCreateDTOToRoute(RouteCreateDTO dto) {
        Route route = new Route();
        route.setName(dto.name());
        return route;
    }

    public Route routeUpdateDTOToRoute(RouteUpdateDTO dto) {
        Route route = new Route();
        route.setName(dto.name());
        return route;
    }
}
