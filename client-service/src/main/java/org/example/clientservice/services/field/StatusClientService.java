package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.StatusClientNotFoundException;
import org.example.clientservice.repositories.field.StatusClientRepository;
import org.example.clientservice.models.field.StatusClient;
import org.example.clientservice.services.impl.IStatusClientService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusClientService implements IStatusClientService {
    private final StatusClientRepository statusClientRepository;

    @Override
    @Cacheable(value = "statusClients", key = "#id")
    public StatusClient getStatusClient(Long id) {
        return statusClientRepository.findById(id)
                .orElseThrow(() ->
                        new StatusClientNotFoundException(String.format("StatusClient not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "statusClients", key = "'allStatusClients'")
    public List<StatusClient> getAllStatusClients() {
        return (List<StatusClient>) statusClientRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"statusClients", "statusClientNames", "statusClientSearch"}, allEntries = true)
    public StatusClient createStatusClient(StatusClient statusClient) {
        return statusClientRepository.save(statusClient);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"statusClients", "statusClientNames", "statusClientSearch"}, allEntries = true)
    public StatusClient updateStatusClient(Long id, StatusClient statusClient) {
        StatusClient existingStatusClient = findStatusClient(id);
        existingStatusClient.setName(statusClient.getName());
        return statusClientRepository.save(existingStatusClient);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"statusClients", "statusClientNames", "statusClientSearch"}, allEntries = true)
    public void deleteStatusClient(Long id) {
        StatusClient statusClient = findStatusClient(id);
        statusClientRepository.delete(statusClient);
    }

    @Override
    @Cacheable(value = "statusClientNames", key = "'statusClientNames'")
    public Map<Long, String> getStatusClientNames() {
        List<StatusClient> statusClient = (List<StatusClient>) statusClientRepository.findAll();
        return statusClient.stream()
                .collect(Collectors.toMap(StatusClient::getId, StatusClient::getName));
    }

    @Override
    @Cacheable(value = "statusClientSearch", key = "#query")
    public List<StatusClient> findByNameContaining(String query) {
        return statusClientRepository.findByNameContainingIgnoreCase(query);
    }

    private StatusClient findStatusClient(Long id) {
        return statusClientRepository.findById(id).orElseThrow(() ->
                new StatusClientNotFoundException(String.format("Status client not found with id: %d", id)));
    }
}

