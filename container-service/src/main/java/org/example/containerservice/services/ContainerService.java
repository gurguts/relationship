package org.example.containerservice.services;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerService implements IContainerService {
    private final ContainerRepository containerRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"containers"}, allEntries = true)
    public Container createContainer(Container container) {
        log.info("Creating Container: {}", container);
        return containerRepository.save(container);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"containers"}, allEntries = true)
    public Container updateContainer(Long id, Container updatingContainer) {
        Container existingContainer = containerRepository.findById(id)
                .orElseThrow(() -> new ContainerNotFoundException(String.format("Container with id %d not found", id)));
        existingContainer.setName(updatingContainer.getName());
        return containerRepository.save(existingContainer);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"containers"}, allEntries = true)
    public void deleteContainer(Long id) {
        log.info("Deleting Container with id: {}", id);
        containerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "containers", key = "'allContainers'")
    public List<Container> getAllContainers() {
        return containerRepository.findAll();
    }

    @Override
    @Cacheable(value = "containers", key = "#id")
    public Container getContainerById(Long id) {
        log.debug("Retrieving Container with id: {}", id);
        return containerRepository.findById(id)
                .orElseThrow(() -> new ContainerNotFoundException(String.format("Container with id %d not found", id)));
    }

}
