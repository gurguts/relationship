package org.example.purchaseservice.services.vehicle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.example.purchaseservice.repositories.VehicleDestinationCountryRepository;
import org.example.purchaseservice.services.impl.IVehicleDestinationCountryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleDestinationCountryService implements IVehicleDestinationCountryService {
    
    private final VehicleDestinationCountryRepository vehicleDestinationCountryRepository;
    
    @Override
    @Transactional
    public VehicleDestinationCountry createVehicleDestinationCountry(VehicleDestinationCountry country) {
        log.info("Creating new vehicle destination country: name={}", country.getName());
        if (vehicleDestinationCountryRepository.existsByName(country.getName())) {
            throw new IllegalArgumentException("Destination country with name '" + country.getName() + "' already exists");
        }
        VehicleDestinationCountry saved = vehicleDestinationCountryRepository.save(country);
        log.info("Vehicle destination country created: id={}", saved.getId());
        return saved;
    }
    
    @Override
    public VehicleDestinationCountry getVehicleDestinationCountry(Long id) {
        return vehicleDestinationCountryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Destination country not found with id: " + id));
    }
    
    @Override
    public List<VehicleDestinationCountry> getAllVehicleDestinationCountries() {
        return vehicleDestinationCountryRepository.findAll();
    }
    
    @Override
    @Transactional
    public VehicleDestinationCountry updateVehicleDestinationCountry(Long id, VehicleDestinationCountry updateData) {
        log.info("Updating vehicle destination country: id={}", id);
        VehicleDestinationCountry country = getVehicleDestinationCountry(id);
        
        if (!country.getName().equals(updateData.getName()) && 
            vehicleDestinationCountryRepository.existsByName(updateData.getName())) {
            throw new IllegalArgumentException("Destination country with name '" + updateData.getName() + "' already exists");
        }
        
        country.setName(updateData.getName());
        VehicleDestinationCountry saved = vehicleDestinationCountryRepository.save(country);
        log.info("Vehicle destination country updated: id={}", saved.getId());
        return saved;
    }
    
    @Override
    @Transactional
    public void deleteVehicleDestinationCountry(Long id) {
        log.info("Deleting vehicle destination country: id={}", id);
        if (!vehicleDestinationCountryRepository.existsById(id)) {
            throw new IllegalArgumentException("Destination country not found with id: " + id);
        }
        vehicleDestinationCountryRepository.deleteById(id);
        log.info("Vehicle destination country deleted: id={}", id);
    }
}
