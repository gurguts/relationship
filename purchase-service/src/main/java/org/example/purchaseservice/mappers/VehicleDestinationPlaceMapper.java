package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationPlaceCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDestinationPlaceDTO;
import org.springframework.stereotype.Component;

@Component
public class VehicleDestinationPlaceMapper {
    
    public VehicleDestinationPlaceDTO vehicleDestinationPlaceToVehicleDestinationPlaceDTO(VehicleDestinationPlace place) {
        if (place == null) {
            return null;
        }
        VehicleDestinationPlaceDTO dto = new VehicleDestinationPlaceDTO();
        dto.setId(place.getId());
        dto.setName(place.getName());
        dto.setCreatedAt(place.getCreatedAt());
        dto.setUpdatedAt(place.getUpdatedAt());
        return dto;
    }
    
    public VehicleDestinationPlace vehicleDestinationPlaceCreateDTOToVehicleDestinationPlace(VehicleDestinationPlaceCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        VehicleDestinationPlace place = new VehicleDestinationPlace();
        place.setName(dto.getName().trim());
        return place;
    }
}
