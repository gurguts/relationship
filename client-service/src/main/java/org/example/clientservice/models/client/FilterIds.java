package org.example.clientservice.models.client;

import org.example.clientservice.models.field.*;

import java.util.List;

public record FilterIds(
        List<Business> businessDTOs, List<Long> businessIds,
        List<Region> regionDTOs, List<Long> regionIds,
        List<Route> routeDTOs, List<Long> routeIds,
        List<Source> sourceDTOs, List<Long> sourceIds,
        List<StatusClient> statusDTOs, List<Long> statusIds
) {
}