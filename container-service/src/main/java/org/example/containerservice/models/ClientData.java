package org.example.containerservice.models;

import org.example.containerservice.models.dto.client.ClientDTO;

import java.util.List;
import java.util.Map;

public record ClientData(List<Long> clientIds, Map<Long, ClientDTO> clientMap) {
}
