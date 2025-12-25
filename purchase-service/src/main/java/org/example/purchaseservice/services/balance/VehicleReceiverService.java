package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverCreateDTO;
import org.example.purchaseservice.repositories.VehicleReceiverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleReceiverService {
    
    private final VehicleReceiverRepository vehicleReceiverRepository;
    
    @Transactional
    public VehicleReceiver createVehicleReceiver(VehicleReceiverCreateDTO dto) {
        log.info("Creating new vehicle receiver: name={}", dto.getName());
        
        if (vehicleReceiverRepository.existsByName(dto.getName())) {
            throw new PurchaseException("VEHICLE_RECEIVER_ALREADY_EXISTS",
                    String.format("Vehicle receiver with name '%s' already exists", dto.getName()));
        }
        
        VehicleReceiver receiver = new VehicleReceiver();
        receiver.setName(dto.getName().trim());
        
        VehicleReceiver saved = vehicleReceiverRepository.save(receiver);
        log.info("Vehicle receiver created: id={}", saved.getId());
        
        return saved;
    }
    
    @Transactional
    public VehicleReceiver updateVehicleReceiver(Long receiverId, VehicleReceiverCreateDTO dto) {
        log.info("Updating vehicle receiver: id={}", receiverId);
        
        VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                        String.format("Vehicle receiver not found: id=%d", receiverId)));
        
        String trimmedName = dto.getName().trim();
        if (!receiver.getName().equals(trimmedName) && vehicleReceiverRepository.existsByName(trimmedName)) {
            throw new PurchaseException("VEHICLE_RECEIVER_ALREADY_EXISTS",
                    String.format("Vehicle receiver with name '%s' already exists", trimmedName));
        }
        
        receiver.setName(trimmedName);
        
        VehicleReceiver saved = vehicleReceiverRepository.save(receiver);
        log.info("Vehicle receiver updated: id={}", saved.getId());
        return saved;
    }
    
    @Transactional(readOnly = true)
    public VehicleReceiver getVehicleReceiver(Long receiverId) {
        return vehicleReceiverRepository.findById(receiverId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                        String.format("Vehicle receiver not found: id=%d", receiverId)));
    }
    
    @Transactional(readOnly = true)
    public List<VehicleReceiver> getAllVehicleReceivers() {
        return vehicleReceiverRepository.findAllByOrderByNameAsc();
    }
    
    @Transactional
    public void deleteVehicleReceiver(Long receiverId) {
        log.info("Deleting vehicle receiver: id={}", receiverId);
        
        VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                        String.format("Vehicle receiver not found: id=%d", receiverId)));
        
        vehicleReceiverRepository.delete(receiver);
        log.info("Vehicle receiver deleted: id={}", receiverId);
    }
}

