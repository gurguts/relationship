package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.dto.balance.VehicleSenderCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleSenderDTO;
import org.springframework.stereotype.Component;

@Component
public class VehicleSenderMapper {

    public VehicleSenderDTO vehicleSenderToVehicleSenderDTO(@NonNull VehicleSender sender) {
        return VehicleSenderDTO.builder()
                .id(sender.getId())
                .name(sender.getName())
                .createdAt(sender.getCreatedAt())
                .updatedAt(sender.getUpdatedAt())
                .build();
    }

    public VehicleSender vehicleSenderCreateDTOToVehicleSender(@NonNull VehicleSenderCreateDTO dto) {
        VehicleSender sender = new VehicleSender();
        sender.setName(dto.getName() != null ? dto.getName().trim() : null);
        return sender;
    }
}

