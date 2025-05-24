package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.RegionNotFoundException;
import org.example.clientservice.models.field.Region;
import org.example.clientservice.repositories.field.RegionRepository;
import org.example.clientservice.services.impl.IRegionService;
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
public class RegionService implements IRegionService {
    private final RegionRepository regionRepository;

    @Override
    @Cacheable(value = "regions", key = "#id")
    public Region getRegion(Long id) {
        return regionRepository.findById(id).orElseThrow(() ->
                new RegionNotFoundException(String.format("Region not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "regions", key = "'allRegions'")
    public List<Region> getAllRegions() {
        return (List<Region>) regionRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"regions", "regionNames", "regionSearch"}, allEntries = true)
    public Region createRegion(Region region) {
        region.setId(null);
        return regionRepository.save(region);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"regions", "regionNames", "regionSearch"}, allEntries = true)
    public Region updateRegion(Long id, Region region) {
        Region oldRegion = findRegion(id);
        oldRegion.setName(region.getName());
        return regionRepository.save(oldRegion);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"regions", "regionNames", "regionSearch"}, allEntries = true)
    public void deleteRegion(Long id) {
        Region region = findRegion(id);
        regionRepository.delete(region);
    }

    @Override
    @Cacheable(value = "regionNames", key = "'regionNames'")
    public Map<Long, String> getRegionNames() {
        List<Region> regions = (List<Region>) regionRepository.findAll();
        return regions.stream()
                .collect(Collectors.toMap(Region::getId, Region::getName));
    }

    @Override
    @Cacheable(value = "regionSearch", key = "#query")
    public List<Region> findByNameContaining(String query) {
        return regionRepository.findByNameContainingIgnoreCase(query);
    }

    private Region findRegion(Long id) {
        return regionRepository.findById(id).orElseThrow(() ->
                new RegionNotFoundException(String.format("Region not found with id: %d", id)));
    }
}
