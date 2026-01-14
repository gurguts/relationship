package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleReceiver;

import java.util.List;

public interface IVehicleReceiverService {
    VehicleReceiver createVehicleReceiver(@NonNull VehicleReceiver receiver);
    
    VehicleReceiver updateVehicleReceiver(@NonNull Long receiverId, @NonNull VehicleReceiver updateData);
    
    VehicleReceiver getVehicleReceiver(@NonNull Long receiverId);
    
    List<VehicleReceiver> getAllVehicleReceivers();
    
    void deleteVehicleReceiver(@NonNull Long receiverId);
}
