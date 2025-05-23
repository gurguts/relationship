package org.example.statusclientservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.statusclientservice.exceptions.StatusClientException;
import org.example.statusclientservice.exceptions.StatusClientNotFoundException;
import org.example.statusclientservice.models.StatusClient;
import org.example.statusclientservice.repositories.StatusClientRepository;
import org.example.statusclientservice.services.impl.IStatusClientService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusClientService implements IStatusClientService {
    private final StatusClientRepository statusClientRepository;

    @Override
    public StatusClient getStatusClient(Long id) {
        return statusClientRepository.findById(id)
                .orElseThrow(() -> new StatusClientNotFoundException("StatusClient not found with id: " + id));
    }

    @Override
    public List<StatusClient> getAllStatusClients() {
        return (List<StatusClient>) statusClientRepository.findAll();
    }

    @Override
    public StatusClient createStatusClient(StatusClient statusClient) {
        return statusClientRepository.save(statusClient);
    }

    @Override
    public StatusClient updateStatusClient(Long id, StatusClient statusClient) {
        StatusClient existingStatusClient = getStatusClient(id);
        existingStatusClient.setName(statusClient.getName());
        return statusClientRepository.save(existingStatusClient);
    }

    @Override
    public void deleteStatusClient(Long id) {
        StatusClient statusClient = getStatusClient(id);
        statusClientRepository.delete(statusClient);
    }

    @Override
    public Map<Long, String> getStatusClientNames() {
        List<StatusClient> statusClient = (List<StatusClient>) statusClientRepository.findAll();
        return statusClient.stream()
                .collect(Collectors.toMap(StatusClient::getId, StatusClient::getName));
    }

    @Override
    public List<StatusClient> findByNameContaining(String query) {
        return statusClientRepository.findByNameContainingIgnoreCase(query);
    }
}

