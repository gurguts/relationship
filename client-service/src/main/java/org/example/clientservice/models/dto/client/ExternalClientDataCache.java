package org.example.clientservice.models.dto.client;

import lombok.Getter;
import org.example.clientservice.models.field.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class ExternalClientDataCache {
    private final Map<Long, Business> businessMap;
    private final Map<Long, Route> routeMap;
    private final Map<Long, Region> regionMap;
    private final Map<Long, StatusClient> statusClientMap;
    private final Map<Long, Source> sourceMap;
    private final Map<Long, ClientProduct> clientProductMap;

    public ExternalClientDataCache(
            List<Business> businesses,
            List<Route> routes,
            List<Region> regions,
            List<StatusClient> statusClients,
            List<Source> sources,
            List<ClientProduct> clientProducts
    ) {
        this.businessMap = businesses.stream().collect(Collectors.toMap(Business::getId, Function.identity()));
        this.routeMap = routes.stream().collect(Collectors.toMap(Route::getId, Function.identity()));
        this.regionMap = regions.stream().collect(Collectors.toMap(Region::getId, Function.identity()));
        this.statusClientMap = statusClients.stream().collect(Collectors.toMap(StatusClient::getId, Function.identity()));
        this.sourceMap = sources.stream().collect(Collectors.toMap(Source::getId, Function.identity()));
        this.clientProductMap = clientProducts.stream().collect(Collectors.toMap(ClientProduct::getId, Function.identity()));
    }

}
