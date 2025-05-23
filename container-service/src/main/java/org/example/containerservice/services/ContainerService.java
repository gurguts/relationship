package org.example.containerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerNotFoundException;
import org.example.containerservice.models.Container;
import org.example.containerservice.repositories.ContainerRepository;
import org.example.containerservice.services.impl.IContainerService;
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
    public Container createContainer(Container container) {
        log.info("Creating Container: {}", container);
        return containerRepository.save(container);
    }

    @Override
    @Transactional
    public Container updateContainer(Long id, Container updatingContainer) {
        Container existingContainer = getContainerById(id);
        existingContainer.setName(updatingContainer.getName());
        return containerRepository.save(existingContainer);
    }

    @Override
    @Transactional
    public void deleteContainer(Long id) {
        log.info("Deleting Container with id: {}", id);
        containerRepository.deleteById(id);
    }

    @Override
    public List<Container> getAllContainers() {
        return containerRepository.findAll();
    }

    @Override
    public Container getContainerById(Long id) {
        log.info("Retrieving Container with id: {}", id);
        return containerRepository.findById(id)
                .orElseThrow(() -> new ContainerNotFoundException(String.format("Container with id %d not found", id)));
    }

}
