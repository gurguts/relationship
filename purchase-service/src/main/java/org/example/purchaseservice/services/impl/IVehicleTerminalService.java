package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.balance.VehicleTerminal;

import java.util.List;

public interface IVehicleTerminalService {
    VehicleTerminal createVehicleTerminal(VehicleTerminal terminal);
    
    VehicleTerminal getVehicleTerminal(Long id);
    
    List<VehicleTerminal> getAllVehicleTerminals();
    
    VehicleTerminal updateVehicleTerminal(Long id, VehicleTerminal updateData);
    
    void deleteVehicleTerminal(Long id);
}
