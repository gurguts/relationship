package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.repositories.VehicleReceiverRepository;
import org.example.purchaseservice.services.impl.IVehicleReceiverService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleReceiverService implements IVehicleReceiverService {
    
    private final VehicleReceiverRepository vehicleReceiverRepository;
    
    @Override
    @Transactional
    public VehicleReceiver createVehicleReceiver(@NonNull VehicleReceiver receiver) {
        if (receiver.getName() == null || receiver.getName().trim().isEmpty()) {
            throw new PurchaseException("INVALID_RECEIVER_DATA", "Receiver name is required");
        }
        
        String normalizedName = receiver.getName().trim();
        log.info("Creating new vehicle receiver: name={}", normalizedName);
        
        if (vehicleReceiverRepository.existsByName(normalizedName)) {
            throw new PurchaseException("VEHICLE_RECEIVER_ALREADY_EXISTS",
                    String.format("Vehicle receiver with name '%s' already exists", normalizedName));
        }
        
        receiver.setName(normalizedName);
        VehicleReceiver saved = vehicleReceiverRepository.save(receiver);
        log.info("Vehicle receiver created: id={}", saved.getId());
        
        return saved;
    }
    
    @Override
    @Transactional
    public VehicleReceiver updateVehicleReceiver(@NonNull Long receiverId, @NonNull VehicleReceiver updateData) {
        log.info("Updating vehicle receiver: id={}", receiverId);
        
        VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                        String.format("Vehicle receiver not found: id=%d", receiverId)));
        
        if (updateData.getName() == null || updateData.getName().trim().isEmpty()) {
            throw new PurchaseException("INVALID_RECEIVER_DATA", "Receiver name cannot be empty");
        }
        
        String normalizedName = updateData.getName().trim();
        
        if (!receiver.getName().equals(normalizedName)) {
            if (vehicleReceiverRepository.existsByNameAndIdNot(normalizedName, receiverId)) {
                throw new PurchaseException("VEHICLE_RECEIVER_ALREADY_EXISTS",
                        String.format("Vehicle receiver with name '%s' already exists", normalizedName));
            }
            receiver.setName(normalizedName);
        }
        
        VehicleReceiver saved = vehicleReceiverRepository.save(receiver);
        log.info("Vehicle receiver updated: id={}", saved.getId());
        return saved;
    }
    
    @Override
    @Transactional(readOnly = true)
    public VehicleReceiver getVehicleReceiver(@NonNull Long receiverId) {
        return vehicleReceiverRepository.findById(receiverId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                        String.format("Vehicle receiver not found: id=%d", receiverId)));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VehicleReceiver> getAllVehicleReceivers() {
        return vehicleReceiverRepository.findAllByOrderByNameAsc();
    }
    
    @Override
    @Transactional
    public void deleteVehicleReceiver(@NonNull Long receiverId) {
        log.info("Deleting vehicle receiver: id={}", receiverId);
        
        VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                        String.format("Vehicle receiver not found: id=%d", receiverId)));
        
        vehicleReceiverRepository.delete(receiver);
        log.info("Vehicle receiver deleted: id={}", receiverId);
    }
}

