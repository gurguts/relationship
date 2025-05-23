package org.example.statusclientservice.services.impl;

import org.example.statusclientservice.models.StatusClient;

import java.util.List;
import java.util.Map;

public interface IStatusClientService {
    StatusClient getStatusClient(Long id);
    List<StatusClient> getAllStatusClients();
    StatusClient createStatusClient(StatusClient statusClient);
    StatusClient updateStatusClient(Long id, StatusClient statusClient);
    void deleteStatusClient(Long id);

    Map<Long, String> getStatusClientNames();

    List<StatusClient> findByNameContaining(String query);
}
