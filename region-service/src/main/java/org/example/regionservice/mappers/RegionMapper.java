package org.example.regionservice.mappers;

import org.example.regionservice.models.Region;
import org.example.regionservice.models.dto.RegionCreateDTO;
import org.example.regionservice.models.dto.RegionDTO;
import org.example.regionservice.models.dto.RegionUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class RegionMapper {
    public Region regionDTOToRegion(RegionDTO regionDTO) {
        Region region = new Region();
        region.setId(regionDTO.getId());
        region.setName(regionDTO.getName());
        return region;
    }

    public RegionDTO regionToRegionDTO(Region region) {
        RegionDTO regionDTO = new RegionDTO();
        regionDTO.setId(region.getId());
        regionDTO.setName(region.getName());
        return regionDTO;
    }

    public Region regionCreateDTOToRegion(RegionCreateDTO dto) {
        Region region = new Region();
        region.setName(dto.getName());
        return region;
    }


    public Region regionUpdateDTOToRegion(RegionUpdateDTO dto) {
        Region region = new Region();
        region.setName(dto.getName());
        return region;
    }
}
