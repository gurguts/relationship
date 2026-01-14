package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerNotFoundException;
import org.example.containerservice.models.Container;
import org.example.containerservice.repositories.ContainerRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerService implements IContainerService {

    private static final String CACHE_NAME_CONTAINERS = "containers";
    private static final String CACHE_KEY_ALL_CONTAINERS = "'allContainers'";
    private static final String MESSAGE_CONTAINER_NOT_FOUND = "Container with id %d not found";

    private final ContainerRepository containerRepository;

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME_CONTAINERS, allEntries = true)
    public Container createContainer(@NonNull Container container) {
        log.info("Creating container: name={}", container.getName());
        return containerRepository.save(container);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME_CONTAINERS, allEntries = true)
    public Container updateContainer(@NonNull Long id, @NonNull Container updatingContainer) {
        log.info("Updating container: id={}", id);
        Container existingContainer = containerRepository.findById(id)
                .orElseThrow(() -> new ContainerNotFoundException(String.format(MESSAGE_CONTAINER_NOT_FOUND, id)));
        existingContainer.setName(updatingContainer.getName());
        return containerRepository.save(existingContainer);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME_CONTAINERS, allEntries = true)
    public void deleteContainer(@NonNull Long id) {
        log.info("Deleting container: id={}", id);
        containerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME_CONTAINERS, key = CACHE_KEY_ALL_CONTAINERS)
    public List<Container> getAllContainers() {
        return containerRepository.findAll();
    }

    @Override
    @Cacheable(value = CACHE_NAME_CONTAINERS, key = "#id")
    public Container getContainerById(@NonNull Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new ContainerNotFoundException(String.format(MESSAGE_CONTAINER_NOT_FOUND, id)));
    }
}
