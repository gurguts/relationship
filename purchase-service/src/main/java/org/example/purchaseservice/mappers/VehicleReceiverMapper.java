package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleReceiverDTO;
import org.springframework.stereotype.Component;

@Component
public class VehicleReceiverMapper {

    public VehicleReceiverDTO vehicleReceiverToVehicleReceiverDTO(@NonNull VehicleReceiver receiver) {
        return VehicleReceiverDTO.builder()
                .id(receiver.getId())
                .name(receiver.getName())
                .createdAt(receiver.getCreatedAt())
                .updatedAt(receiver.getUpdatedAt())
                .build();
    }

    public VehicleReceiver vehicleReceiverCreateDTOToVehicleReceiver(@NonNull VehicleReceiverCreateDTO dto) {
        VehicleReceiver receiver = new VehicleReceiver();
        receiver.setName(dto.getName() != null ? dto.getName().trim() : null);
        return receiver;
    }
}

