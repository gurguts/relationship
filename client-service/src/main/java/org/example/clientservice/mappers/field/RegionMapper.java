package org.example.clientservice.mappers.field;

import org.example.clientservice.models.dto.fields.RegionCreateDTO;
import org.example.clientservice.models.dto.fields.RegionDTO;
import org.example.clientservice.models.dto.fields.RegionUpdateDTO;
import org.example.clientservice.models.field.Region;
import org.springframework.stereotype.Component;

@Component
public class RegionMapper {

    public RegionDTO regionToRegionDTO(Region region) {
        RegionDTO regionDTO = new RegionDTO();
        regionDTO.setId(region.getId());
        regionDTO.setName(region.getName());
        return regionDTO;
    }

    public Region regionCreateDTOToRegion(RegionCreateDTO dto) {
        Region region = new Region();
        region.setName(dto.name());
        return region;
    }


    public Region regionUpdateDTOToRegion(RegionUpdateDTO dto) {
        Region region = new Region();
        region.setName(dto.name());
        return region;
    }
}
