package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.example.purchaseservice.repositories.VehicleDestinationPlaceRepository;
import org.example.purchaseservice.services.impl.IVehicleDestinationPlaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleDestinationPlaceService implements IVehicleDestinationPlaceService {
    
    private final VehicleDestinationPlaceRepository vehicleDestinationPlaceRepository;
    
    @Override
    @Transactional
    public VehicleDestinationPlace createVehicleDestinationPlace(VehicleDestinationPlace place) {
        log.info("Creating new vehicle destination place: name={}", place.getName());
        if (vehicleDestinationPlaceRepository.existsByName(place.getName())) {
            throw new IllegalArgumentException("Destination place with name '" + place.getName() + "' already exists");
        }
        VehicleDestinationPlace saved = vehicleDestinationPlaceRepository.save(place);
        log.info("Vehicle destination place created: id={}", saved.getId());
        return saved;
    }
    
    @Override
    public VehicleDestinationPlace getVehicleDestinationPlace(Long id) {
        return vehicleDestinationPlaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Destination place not found with id: " + id));
    }
    
    @Override
    public List<VehicleDestinationPlace> getAllVehicleDestinationPlaces() {
        return vehicleDestinationPlaceRepository.findAll();
    }
    
    @Override
    @Transactional
    public VehicleDestinationPlace updateVehicleDestinationPlace(Long id, VehicleDestinationPlace updateData) {
        log.info("Updating vehicle destination place: id={}", id);
        VehicleDestinationPlace place = getVehicleDestinationPlace(id);
        
        if (!place.getName().equals(updateData.getName()) && 
            vehicleDestinationPlaceRepository.existsByName(updateData.getName())) {
            throw new IllegalArgumentException("Destination place with name '" + updateData.getName() + "' already exists");
        }
        
        place.setName(updateData.getName());
        VehicleDestinationPlace saved = vehicleDestinationPlaceRepository.save(place);
        log.info("Vehicle destination place updated: id={}", saved.getId());
        return saved;
    }
    
    @Override
    @Transactional
    public void deleteVehicleDestinationPlace(Long id) {
        log.info("Deleting vehicle destination place: id={}", id);
        if (!vehicleDestinationPlaceRepository.existsById(id)) {
            throw new IllegalArgumentException("Destination place not found with id: " + id);
        }
        vehicleDestinationPlaceRepository.deleteById(id);
        log.info("Vehicle destination place deleted: id={}", id);
    }
}
