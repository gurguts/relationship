package org.example.sourceservice.restControllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sourceservice.models.Source;
import org.example.sourceservice.models.dto.SourceCreateDTO;
import org.example.sourceservice.models.dto.SourceDTO;
import org.example.sourceservice.models.dto.SourceUpdateDTO;
import org.example.sourceservice.services.impl.ISourceService;
import org.example.sourceservice.mappers.SourceMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/source")
@RequiredArgsConstructor
@Slf4j
public class SourceController {
    private final ISourceService sourceService;
    private final SourceMapper sourceMapper;

    @GetMapping("/{id}")
    public ResponseEntity<SourceDTO> getSource(@PathVariable Long id) {
        Source source = sourceService.getSource(id);
        return ResponseEntity.ok(sourceMapper.sourceToSourceDTO(source));
    }

    @GetMapping
    public ResponseEntity<List<SourceDTO>> getAllSources() {
        List<Source> sources = sourceService.getAllSources();
        List<SourceDTO> sourceDTOs = sources.stream()
                .map(sourceMapper::sourceToSourceDTO)
                .toList();
        return ResponseEntity.ok(sourceDTOs);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PostMapping
    public ResponseEntity<SourceDTO> createSource(@RequestBody SourceCreateDTO sourceCreateDTO) {
        Source source = sourceMapper.sourceCreateDTOtoSource(sourceCreateDTO);
        SourceDTO createdSource = sourceMapper.sourceToSourceDTO(sourceService.createSource(source));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSource.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdSource);
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<SourceDTO> updateSource(@PathVariable Long id, @RequestBody SourceUpdateDTO sourceUpdateDTO) {
        Source source = sourceMapper.sourceUpdateDTOtoSource(sourceUpdateDTO);
        Source updatedSource = sourceService.updateSource(id, source);
        return ResponseEntity.ok(sourceMapper.sourceToSourceDTO(updatedSource));
    }

    @PreAuthorize("hasAuthority('settings:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable Long id) {
        sourceService.deleteSource(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, String>> getSourceNames() {
        return ResponseEntity.ok(sourceService.getSourceNames());
    }

    @GetMapping("/ids")
    public ResponseEntity<List<SourceDTO>> findByNameContaining(@RequestParam String query) {
        return ResponseEntity.ok(sourceService.findByNameContaining(query).stream().map(sourceMapper::sourceToSourceDTO).toList());
    }
}
