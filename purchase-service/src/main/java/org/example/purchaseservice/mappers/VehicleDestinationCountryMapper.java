package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationCountryCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationCountryDTO;
import org.springframework.stereotype.Component;

@Component
public class VehicleDestinationCountryMapper {
    
    public VehicleDestinationCountryDTO vehicleDestinationCountryToVehicleDestinationCountryDTO(VehicleDestinationCountry country) {
        if (country == null) {
            return null;
        }
        VehicleDestinationCountryDTO dto = new VehicleDestinationCountryDTO();
        dto.setId(country.getId());
        dto.setName(country.getName());
        dto.setCreatedAt(country.getCreatedAt());
        dto.setUpdatedAt(country.getUpdatedAt());
        return dto;
    }
    
    public VehicleDestinationCountry vehicleDestinationCountryCreateDTOToVehicleDestinationCountry(VehicleDestinationCountryCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        VehicleDestinationCountry country = new VehicleDestinationCountry();
        country.setName(dto.getName().trim());
        return country;
    }
}
