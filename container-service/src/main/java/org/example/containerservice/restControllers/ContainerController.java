package org.example.containerservice.restControllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.dto.container.ContainerCreateDTO;
import org.example.containerservice.models.dto.container.ContainerDTO;
import org.example.containerservice.models.dto.container.ContainerUpdateDTO;
import org.example.containerservice.services.impl.IContainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/container")
@RequiredArgsConstructor
@Slf4j
public class ContainerController {
    private final IContainerService containerService;
    private final ContainerMapper containerMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ContainerDTO> findById(@PathVariable Long id) {
        ContainerDTO containerDTO = containerMapper.containerToContainerDTO(containerService.getContainerById(id));
        return ResponseEntity.ok(containerDTO);
    }

    @GetMapping
    public ResponseEntity<List<ContainerDTO>> findAll() {
        List<Container> barrelTypes = containerService.getAllContainers();
        List<ContainerDTO> containerDTOS = barrelTypes.stream()
                .map(containerMapper::containerToContainerDTO)
                .toList();
        return ResponseEntity.ok(containerDTOS);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<ContainerDTO> create(@RequestBody @Valid ContainerCreateDTO containerCreateDTO) {
        Container barrelType = containerMapper.containerCreateDTOToContainer(containerCreateDTO);
        ContainerDTO createdContainer =
                containerMapper.containerToContainerDTO(containerService.createContainer(barrelType));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdContainer.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdContainer);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ContainerDTO> update(@PathVariable Long id,
                                               @RequestBody @Valid ContainerUpdateDTO containerUpdateDTO) {
        Container barrelType = containerMapper.containerUpdateDTOToContainer(containerUpdateDTO);
        Container response = containerService.updateContainer(id, barrelType);
        return ResponseEntity.ok(containerMapper.containerToContainerDTO(response));
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        containerService.deleteContainer(id);
        return ResponseEntity.noContent().build();
    }
}
