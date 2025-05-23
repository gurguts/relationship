package org.example.regionservice.services.impl;

import org.example.regionservice.models.Region;

import java.util.List;
import java.util.Map;

public interface IRegionService {
    Region getRegion(Long id);
    List<Region> getAllRegions();
    Region createRegion(Region region);
    Region updateRegion(Long id, Region region);
    void deleteRegion(Long id);

    Map<Long, String> getRegionNames();

    List<Region> findByNameContaining(String query);
}
