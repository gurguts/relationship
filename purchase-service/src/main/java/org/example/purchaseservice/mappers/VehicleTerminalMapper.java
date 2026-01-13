package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.example.purchaseservice.models.dto.balance.VehicleTerminalCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleTerminalDTO;
import org.springframework.stereotype.Component;

@Component
public class VehicleTerminalMapper {
    
    public VehicleTerminalDTO vehicleTerminalToVehicleTerminalDTO(VehicleTerminal terminal) {
        if (terminal == null) {
            return null;
        }
        VehicleTerminalDTO dto = new VehicleTerminalDTO();
        dto.setId(terminal.getId());
        dto.setName(terminal.getName());
        dto.setCreatedAt(terminal.getCreatedAt());
        dto.setUpdatedAt(terminal.getUpdatedAt());
        return dto;
    }
    
    public VehicleTerminal vehicleTerminalCreateDTOToVehicleTerminal(VehicleTerminalCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        VehicleTerminal terminal = new VehicleTerminal();
        terminal.setName(dto.getName().trim());
        return terminal;
    }
}
