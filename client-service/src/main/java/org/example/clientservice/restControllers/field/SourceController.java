package org.example.clientservice.restControllers.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.mappers.field.SourceMapper;
import org.example.clientservice.models.dto.fields.SourceCreateDTO;
import org.example.clientservice.models.dto.fields.SourceDTO;
import org.example.clientservice.models.dto.fields.SourceUpdateDTO;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.ISourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PreAuthorize("hasAuthority('settings_client:create')")
    @PostMapping
    public ResponseEntity<SourceDTO> createSource(@RequestBody SourceCreateDTO sourceCreateDTO) {
        if (sourceCreateDTO.getUserId() == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long currentUserId = authentication != null && authentication.getDetails() instanceof Long ? 
                    (Long) authentication.getDetails() : null;
            sourceCreateDTO.setUserId(currentUserId);
        }
        
        Source source = sourceMapper.sourceCreateDTOtoSource(sourceCreateDTO);
        SourceDTO createdSource = sourceMapper.sourceToSourceDTO(sourceService.createSource(source));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSource.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdSource);
    }

    @PreAuthorize("hasAuthority('settings_client:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<SourceDTO> updateSource(@PathVariable Long id, @RequestBody SourceUpdateDTO sourceUpdateDTO) {
        Source source = sourceMapper.sourceUpdateDTOtoSource(sourceUpdateDTO);
        Source updatedSource = sourceService.updateSource(id, source);
        return ResponseEntity.ok(sourceMapper.sourceToSourceDTO(updatedSource));
    }

    @PreAuthorize("hasAuthority('settings_client:delete')")
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
        return ResponseEntity.ok(sourceService.findByNameContaining(query).stream().map(
                sourceMapper::sourceToSourceDTO).toList());
    }
}
