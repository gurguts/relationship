package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.dto.balance.VehicleSenderCreateDTO;
import org.example.purchaseservice.repositories.VehicleSenderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleSenderService {
    
    private final VehicleSenderRepository vehicleSenderRepository;
    
    @Transactional
    public VehicleSender createVehicleSender(VehicleSenderCreateDTO dto) {
        log.info("Creating new vehicle sender: name={}", dto.getName());
        
        if (vehicleSenderRepository.existsByName(dto.getName())) {
            throw new PurchaseException("VEHICLE_SENDER_ALREADY_EXISTS",
                    String.format("Vehicle sender with name '%s' already exists", dto.getName()));
        }
        
        VehicleSender sender = new VehicleSender();
        sender.setName(dto.getName().trim());
        
        VehicleSender saved = vehicleSenderRepository.save(sender);
        log.info("Vehicle sender created: id={}", saved.getId());
        
        return saved;
    }
    
    @Transactional
    public VehicleSender updateVehicleSender(Long senderId, VehicleSenderCreateDTO dto) {
        log.info("Updating vehicle sender: id={}", senderId);
        
        VehicleSender sender = vehicleSenderRepository.findById(senderId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                        String.format("Vehicle sender not found: id=%d", senderId)));
        
        String trimmedName = dto.getName().trim();
        if (!sender.getName().equals(trimmedName) && vehicleSenderRepository.existsByName(trimmedName)) {
            throw new PurchaseException("VEHICLE_SENDER_ALREADY_EXISTS",
                    String.format("Vehicle sender with name '%s' already exists", trimmedName));
        }
        
        sender.setName(trimmedName);
        
        VehicleSender saved = vehicleSenderRepository.save(sender);
        log.info("Vehicle sender updated: id={}", saved.getId());
        return saved;
    }
    
    @Transactional(readOnly = true)
    public VehicleSender getVehicleSender(Long senderId) {
        return vehicleSenderRepository.findById(senderId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                        String.format("Vehicle sender not found: id=%d", senderId)));
    }
    
    @Transactional(readOnly = true)
    public List<VehicleSender> getAllVehicleSenders() {
        return vehicleSenderRepository.findAllByOrderByNameAsc();
    }
    
    @Transactional
    public void deleteVehicleSender(Long senderId) {
        log.info("Deleting vehicle sender: id={}", senderId);
        
        VehicleSender sender = vehicleSenderRepository.findById(senderId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                        String.format("Vehicle sender not found: id=%d", senderId)));
        
        vehicleSenderRepository.delete(sender);
        log.info("Vehicle sender deleted: id={}", senderId);
    }
}

