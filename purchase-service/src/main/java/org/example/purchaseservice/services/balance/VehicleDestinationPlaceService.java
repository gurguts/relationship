package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.example.purchaseservice.repositories.VehicleDestinationPlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleDestinationPlaceService {
    
    private final VehicleDestinationPlaceRepository vehicleDestinationPlaceRepository;
    
    @Transactional
    public VehicleDestinationPlace createVehicleDestinationPlace(VehicleDestinationPlace place) {
        if (vehicleDestinationPlaceRepository.existsByName(place.getName())) {
            throw new IllegalArgumentException("Destination place with name '" + place.getName() + "' already exists");
        }
        return vehicleDestinationPlaceRepository.save(place);
    }
    
    public VehicleDestinationPlace getVehicleDestinationPlace(Long id) {
        return vehicleDestinationPlaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Destination place not found with id: " + id));
    }
    
    public List<VehicleDestinationPlace> getAllVehicleDestinationPlaces() {
        return vehicleDestinationPlaceRepository.findAll();
    }
    
    @Transactional
    public VehicleDestinationPlace updateVehicleDestinationPlace(Long id, VehicleDestinationPlace updateData) {
        VehicleDestinationPlace place = getVehicleDestinationPlace(id);
        
        if (!place.getName().equals(updateData.getName()) && 
            vehicleDestinationPlaceRepository.existsByName(updateData.getName())) {
            throw new IllegalArgumentException("Destination place with name '" + updateData.getName() + "' already exists");
        }
        
        place.setName(updateData.getName());
        return vehicleDestinationPlaceRepository.save(place);
    }
    
    @Transactional
    public void deleteVehicleDestinationPlace(Long id) {
        if (!vehicleDestinationPlaceRepository.existsById(id)) {
            throw new IllegalArgumentException("Destination place not found with id: " + id);
        }
        vehicleDestinationPlaceRepository.deleteById(id);
    }
}
