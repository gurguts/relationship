package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleSender;

import java.util.List;

public interface IVehicleSenderService {
    VehicleSender createVehicleSender(@NonNull VehicleSender sender);
    
    VehicleSender updateVehicleSender(@NonNull Long senderId, @NonNull VehicleSender updateData);
    
    VehicleSender getVehicleSender(@NonNull Long senderId);
    
    List<VehicleSender> getAllVehicleSenders();
    
    void deleteVehicleSender(@NonNull Long senderId);
}
