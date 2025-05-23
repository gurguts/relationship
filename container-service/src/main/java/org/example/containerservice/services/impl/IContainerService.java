package org.example.containerservice.services.impl;

import org.example.containerservice.models.Container;

import java.util.List;

public interface IContainerService {
    Container createContainer(Container container);

    Container updateContainer(Long id, Container updatingContainer);

    void deleteContainer(Long id);

    List<Container> getAllContainers();

    Container getContainerById(Long id);
}
