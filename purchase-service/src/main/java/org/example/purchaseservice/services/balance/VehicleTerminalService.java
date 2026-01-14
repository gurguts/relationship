package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.example.purchaseservice.repositories.VehicleTerminalRepository;
import org.example.purchaseservice.services.impl.IVehicleTerminalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTerminalService implements IVehicleTerminalService {
    
    private final VehicleTerminalRepository vehicleTerminalRepository;
    
    @Override
    @Transactional
    public VehicleTerminal createVehicleTerminal(VehicleTerminal terminal) {
        log.info("Creating new vehicle terminal: name={}", terminal.getName());
        if (vehicleTerminalRepository.existsByName(terminal.getName())) {
            throw new IllegalArgumentException("Terminal with name '" + terminal.getName() + "' already exists");
        }
        VehicleTerminal saved = vehicleTerminalRepository.save(terminal);
        log.info("Vehicle terminal created: id={}", saved.getId());
        return saved;
    }
    
    @Override
    public VehicleTerminal getVehicleTerminal(Long id) {
        return vehicleTerminalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found with id: " + id));
    }
    
    @Override
    public List<VehicleTerminal> getAllVehicleTerminals() {
        return vehicleTerminalRepository.findAll();
    }
    
    @Override
    @Transactional
    public VehicleTerminal updateVehicleTerminal(Long id, VehicleTerminal updateData) {
        log.info("Updating vehicle terminal: id={}", id);
        VehicleTerminal terminal = getVehicleTerminal(id);
        
        if (!terminal.getName().equals(updateData.getName()) && 
            vehicleTerminalRepository.existsByName(updateData.getName())) {
            throw new IllegalArgumentException("Terminal with name '" + updateData.getName() + "' already exists");
        }
        
        terminal.setName(updateData.getName());
        VehicleTerminal saved = vehicleTerminalRepository.save(terminal);
        log.info("Vehicle terminal updated: id={}", saved.getId());
        return saved;
    }
    
    @Override
    @Transactional
    public void deleteVehicleTerminal(Long id) {
        log.info("Deleting vehicle terminal: id={}", id);
        if (!vehicleTerminalRepository.existsById(id)) {
            throw new IllegalArgumentException("Terminal not found with id: " + id);
        }
        vehicleTerminalRepository.deleteById(id);
        log.info("Vehicle terminal deleted: id={}", id);
    }
}
