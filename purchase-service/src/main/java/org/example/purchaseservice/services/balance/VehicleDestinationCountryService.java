package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.example.purchaseservice.repositories.VehicleDestinationCountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleDestinationCountryService {
    
    private final VehicleDestinationCountryRepository vehicleDestinationCountryRepository;
    
    @Transactional
    public VehicleDestinationCountry createVehicleDestinationCountry(VehicleDestinationCountry country) {
        if (vehicleDestinationCountryRepository.existsByName(country.getName())) {
            throw new IllegalArgumentException("Destination country with name '" + country.getName() + "' already exists");
        }
        return vehicleDestinationCountryRepository.save(country);
    }
    
    public VehicleDestinationCountry getVehicleDestinationCountry(Long id) {
        return vehicleDestinationCountryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Destination country not found with id: " + id));
    }
    
    public List<VehicleDestinationCountry> getAllVehicleDestinationCountries() {
        return vehicleDestinationCountryRepository.findAll();
    }
    
    @Transactional
    public VehicleDestinationCountry updateVehicleDestinationCountry(Long id, VehicleDestinationCountry updateData) {
        VehicleDestinationCountry country = getVehicleDestinationCountry(id);
        
        if (!country.getName().equals(updateData.getName()) && 
            vehicleDestinationCountryRepository.existsByName(updateData.getName())) {
            throw new IllegalArgumentException("Destination country with name '" + updateData.getName() + "' already exists");
        }
        
        country.setName(updateData.getName());
        return vehicleDestinationCountryRepository.save(country);
    }
    
    @Transactional
    public void deleteVehicleDestinationCountry(Long id) {
        if (!vehicleDestinationCountryRepository.existsById(id)) {
            throw new IllegalArgumentException("Destination country not found with id: " + id);
        }
        vehicleDestinationCountryRepository.deleteById(id);
    }
}
