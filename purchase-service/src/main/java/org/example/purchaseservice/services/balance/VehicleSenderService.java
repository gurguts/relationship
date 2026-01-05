package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleSender;
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
    public VehicleSender createVehicleSender(@NonNull VehicleSender sender) {
        if (sender.getName() == null || sender.getName().trim().isEmpty()) {
            throw new PurchaseException("INVALID_SENDER_DATA", "Sender name is required");
        }
        
        String normalizedName = sender.getName().trim();
        log.info("Creating new vehicle sender: name={}", normalizedName);
        
        if (vehicleSenderRepository.existsByName(normalizedName)) {
            throw new PurchaseException("VEHICLE_SENDER_ALREADY_EXISTS",
                    String.format("Vehicle sender with name '%s' already exists", normalizedName));
        }
        
        sender.setName(normalizedName);
        VehicleSender saved = vehicleSenderRepository.save(sender);
        log.info("Vehicle sender created: id={}", saved.getId());
        
        return saved;
    }
    
    @Transactional
    public VehicleSender updateVehicleSender(@NonNull Long senderId, @NonNull VehicleSender updateData) {
        log.info("Updating vehicle sender: id={}", senderId);
        
        VehicleSender sender = vehicleSenderRepository.findById(senderId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                        String.format("Vehicle sender not found: id=%d", senderId)));
        
        if (updateData.getName() == null || updateData.getName().trim().isEmpty()) {
            throw new PurchaseException("INVALID_SENDER_DATA", "Sender name cannot be empty");
        }
        
        String normalizedName = updateData.getName().trim();
        
        if (!sender.getName().equals(normalizedName)) {
            if (vehicleSenderRepository.existsByNameAndIdNot(normalizedName, senderId)) {
                throw new PurchaseException("VEHICLE_SENDER_ALREADY_EXISTS",
                        String.format("Vehicle sender with name '%s' already exists", normalizedName));
            }
            sender.setName(normalizedName);
        }
        
        VehicleSender saved = vehicleSenderRepository.save(sender);
        log.info("Vehicle sender updated: id={}", saved.getId());
        return saved;
    }
    
    @Transactional(readOnly = true)
    public VehicleSender getVehicleSender(@NonNull Long senderId) {
        return vehicleSenderRepository.findById(senderId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                        String.format("Vehicle sender not found: id=%d", senderId)));
    }
    
    @Transactional(readOnly = true)
    public List<VehicleSender> getAllVehicleSenders() {
        return vehicleSenderRepository.findAllByOrderByNameAsc();
    }
    
    @Transactional
    public void deleteVehicleSender(@NonNull Long senderId) {
        log.info("Deleting vehicle sender: id={}", senderId);
        
        VehicleSender sender = vehicleSenderRepository.findById(senderId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                        String.format("Vehicle sender not found: id=%d", senderId)));
        
        vehicleSenderRepository.delete(sender);
        log.info("Vehicle sender deleted: id={}", senderId);
    }
}

