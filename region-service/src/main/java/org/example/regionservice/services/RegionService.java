package org.example.regionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.regionservice.exceptions.RegionException;
import org.example.regionservice.exceptions.RegionNotFoundException;
import org.example.regionservice.models.Region;
import org.example.regionservice.repositories.RegionRepository;
import org.example.regionservice.services.impl.IRegionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService implements IRegionService {
    private final RegionRepository regionRepository;

    @Override
    public Region getRegion(Long id) {
        return regionRepository.findById(id).orElseThrow(() -> new RegionNotFoundException("Region not found with id: " + id));
    }

    @Override
    public List<Region> getAllRegions() {
        return (List<Region>) regionRepository.findAll();
    }

    @Override
    public Region createRegion(Region region) {
        region.setId(null);
        return regionRepository.save(region);
    }

    @Override
    public Region updateRegion(Long id, Region region) {
        Region oldRegion = getRegion(id);
        oldRegion.setName(region.getName());
        return regionRepository.save(oldRegion);
    }

    @Override
    public void deleteRegion(Long id) {
        Region region = getRegion(id);
        regionRepository.delete(region);
    }

    @Override
    public Map<Long, String> getRegionNames() {
        List<Region> regions = (List<Region>) regionRepository.findAll();
        return regions.stream()
                .collect(Collectors.toMap(Region::getId, Region::getName));
    }

    @Override
    public List<Region> findByNameContaining(String query) {
        return regionRepository.findByNameContainingIgnoreCase(query);
    }
}
