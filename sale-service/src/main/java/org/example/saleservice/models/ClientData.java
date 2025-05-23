package org.example.saleservice.models;

import org.example.saleservice.models.dto.client.ClientDTO;

import java.util.List;
import java.util.Map;

public record ClientData(List<Long> clientIds, Map<Long, ClientDTO> clientMap) {
}
