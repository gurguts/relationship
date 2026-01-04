package org.example.containerservice.restControllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.dto.container.ContainerCreateDTO;
import org.example.containerservice.models.dto.container.ContainerDTO;
import org.example.containerservice.models.dto.container.ContainerUpdateDTO;
import org.example.containerservice.services.impl.IContainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/container")
@RequiredArgsConstructor
@Validated
public class ContainerController {
    private final IContainerService containerService;
    private final ContainerMapper containerMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ContainerDTO> findById(@PathVariable @Positive Long id) {
        Container container = containerService.getContainerById(id);
        ContainerDTO containerDTO = containerMapper.containerToContainerDTO(container);
        return ResponseEntity.ok(containerDTO);
    }

    @GetMapping
    public ResponseEntity<List<ContainerDTO>> findAll() {
        List<Container> containers = containerService.getAllContainers();
        List<ContainerDTO> containerDTOs = containers.stream()
                .map(containerMapper::containerToContainerDTO)
                .toList();
        return ResponseEntity.ok(containerDTOs);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping
    public ResponseEntity<ContainerDTO> create(@RequestBody @Valid @NonNull ContainerCreateDTO containerCreateDTO) {
        Container container = containerMapper.containerCreateDTOToContainer(containerCreateDTO);
        Container createdContainer = containerService.createContainer(container);
        ContainerDTO createdContainerDTO = containerMapper.containerToContainerDTO(createdContainer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdContainerDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdContainerDTO);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ContainerDTO> update(@PathVariable @Positive Long id,
                                               @RequestBody @Valid @NonNull ContainerUpdateDTO containerUpdateDTO) {
        Container container = containerMapper.containerUpdateDTOToContainer(containerUpdateDTO);
        Container updatedContainer = containerService.updateContainer(id, container);
        ContainerDTO updatedContainerDTO = containerMapper.containerToContainerDTO(updatedContainer);
        return ResponseEntity.ok(updatedContainerDTO);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        containerService.deleteContainer(id);
        return ResponseEntity.noContent().build();
    }
}
