package org.example.purchaseservice.models;

import org.example.purchaseservice.models.dto.client.ClientDTO;

import java.util.List;
import java.util.Map;

public record ClientData(List<Long> clientIds, Map<Long, ClientDTO> clientMap) {
}
