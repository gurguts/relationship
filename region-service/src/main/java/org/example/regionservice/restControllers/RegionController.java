package org.example.regionservice.restControllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.regionservice.mappers.RegionMapper;
import org.example.regionservice.models.Region;
import org.example.regionservice.models.dto.RegionCreateDTO;
import org.example.regionservice.models.dto.RegionDTO;
import org.example.regionservice.models.dto.RegionUpdateDTO;
import org.example.regionservice.services.impl.IRegionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/region")
@RequiredArgsConstructor
@Slf4j
public class RegionController {
    private final IRegionService regionService;
    private final RegionMapper regionMapper;

    @GetMapping("/{id}")
    public ResponseEntity<RegionDTO> getRegion(@PathVariable Long id) {
        RegionDTO regionDTO = regionMapper.regionToRegionDTO(regionService.getRegion(id));
        return ResponseEntity.ok(regionDTO);
    }

    @GetMapping
    public ResponseEntity<List<RegionDTO>> getRegions() {
        List<Region> regions = regionService.getAllRegions();
        List<RegionDTO> dtos = regions.stream()
                .map(regionMapper::regionToRegionDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<RegionDTO> createRegion(@RequestBody @Valid RegionCreateDTO regionCreateDTO) {
        Region region = regionMapper.regionCreateDTOToRegion(regionCreateDTO);
        RegionDTO createdRegion = regionMapper.regionToRegionDTO(regionService.createRegion(region));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRegion.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdRegion);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<RegionDTO> updateRegion(@PathVariable Long id, @RequestBody @Valid RegionUpdateDTO regionUpdateDTO) {
        Region region = regionMapper.regionUpdateDTOToRegion(regionUpdateDTO);
        Region updateRegion = regionService.updateRegion(id, region);
        return ResponseEntity.ok(regionMapper.regionToRegionDTO(updateRegion));
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegion(@PathVariable Long id) {
        regionService.deleteRegion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, String>> getRegionNames() {
        return ResponseEntity.ok(regionService.getRegionNames());
    }

    @GetMapping("/ids")
    public ResponseEntity<List<RegionDTO>> findByNameContaining(@RequestParam String query) {
        return ResponseEntity.ok(regionService.findByNameContaining(query).stream().map(regionMapper::regionToRegionDTO).toList());
    }
}
