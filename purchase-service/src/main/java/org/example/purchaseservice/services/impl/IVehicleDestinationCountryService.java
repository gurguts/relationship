package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.balance.VehicleDestinationCountry;

import java.util.List;

public interface IVehicleDestinationCountryService {
    VehicleDestinationCountry createVehicleDestinationCountry(VehicleDestinationCountry country);
    
    VehicleDestinationCountry getVehicleDestinationCountry(Long id);
    
    List<VehicleDestinationCountry> getAllVehicleDestinationCountries();
    
    VehicleDestinationCountry updateVehicleDestinationCountry(Long id, VehicleDestinationCountry updateData);
    
    void deleteVehicleDestinationCountry(Long id);
}
