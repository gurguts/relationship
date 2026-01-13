package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.example.purchaseservice.repositories.VehicleTerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleTerminalService {
    
    private final VehicleTerminalRepository vehicleTerminalRepository;
    
    @Transactional
    public VehicleTerminal createVehicleTerminal(VehicleTerminal terminal) {
        if (vehicleTerminalRepository.existsByName(terminal.getName())) {
            throw new IllegalArgumentException("Terminal with name '" + terminal.getName() + "' already exists");
        }
        return vehicleTerminalRepository.save(terminal);
    }
    
    public VehicleTerminal getVehicleTerminal(Long id) {
        return vehicleTerminalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found with id: " + id));
    }
    
    public List<VehicleTerminal> getAllVehicleTerminals() {
        return vehicleTerminalRepository.findAll();
    }
    
    @Transactional
    public VehicleTerminal updateVehicleTerminal(Long id, VehicleTerminal updateData) {
        VehicleTerminal terminal = getVehicleTerminal(id);
        
        if (!terminal.getName().equals(updateData.getName()) && 
            vehicleTerminalRepository.existsByName(updateData.getName())) {
            throw new IllegalArgumentException("Terminal with name '" + updateData.getName() + "' already exists");
        }
        
        terminal.setName(updateData.getName());
        return vehicleTerminalRepository.save(terminal);
    }
    
    @Transactional
    public void deleteVehicleTerminal(Long id) {
        if (!vehicleTerminalRepository.existsById(id)) {
            throw new IllegalArgumentException("Terminal not found with id: " + id);
        }
        vehicleTerminalRepository.deleteById(id);
    }
}
