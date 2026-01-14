package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.balance.VehicleDestinationPlace;

import java.util.List;

public interface IVehicleDestinationPlaceService {
    VehicleDestinationPlace createVehicleDestinationPlace(VehicleDestinationPlace place);
    
    VehicleDestinationPlace getVehicleDestinationPlace(Long id);
    
    List<VehicleDestinationPlace> getAllVehicleDestinationPlaces();
    
    VehicleDestinationPlace updateVehicleDestinationPlace(Long id, VehicleDestinationPlace updateData);
    
    void deleteVehicleDestinationPlace(Long id);
}
