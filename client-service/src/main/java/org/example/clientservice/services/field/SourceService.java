package org.example.clientservice.services.field;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.SourceException;
import org.example.clientservice.exceptions.field.SourceNotFoundException;
import org.example.clientservice.mappers.field.SourceMapper;
import org.example.clientservice.models.dto.fields.SourceCreateDTO;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.field.SourceRepository;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.utils.SecurityUtils;
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
public class SourceService implements ISourceService {
    private static final String ERROR_INVALID_ID = "INVALID_ID";
    private static final String ERROR_INVALID_NAME = "INVALID_NAME";
    private static final String ERROR_INVALID_QUERY = "INVALID_QUERY";
    private static final String ERROR_SOURCE_CREATION = "SOURCE_CREATION_ERROR";
    private static final String ERROR_SOURCE_UPDATE = "SOURCE_UPDATE_ERROR";
    private static final String ERROR_SOURCE_DELETION = "SOURCE_DELETION_ERROR";
    
    private final SourceRepository sourceRepository;
    private final SourceMapper sourceMapper;

    @Override
    @Cacheable(value = "sources", key = "#id")
    @NonNull
    public Source getSource(@NonNull Long id) {
        validateId(id);
        return sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(
                        String.format("Source not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "sources", key = "'allSources'")
    @NonNull
    public List<Source> getAllSources() {
        return sourceRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    @NonNull
    public Source createSource(@NonNull Source source) {
        log.info("Creating source: {} for user: {}", source.getName(), source.getUserId());
        
        try {
            validateSource(source);
            return sourceRepository.save(source);
        } catch (Exception e) {
            log.error("Error creating source {}: {}", source.getName(), e.getMessage(), e);
            throw new SourceException(ERROR_SOURCE_CREATION,
                    String.format("Failed to create source: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    @NonNull
    public Source createSource(@NonNull SourceCreateDTO dto) {
        log.info("Creating source from DTO: {}", dto.getName());
        
        try {
            Source source = sourceMapper.sourceCreateDTOtoSource(dto);
            if (source.getUserId() == null) {
                Long currentUserId = SecurityUtils.getCurrentUserId();
                source.setUserId(currentUserId);
            }
            return createSource(source);
        } catch (SourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating source from DTO {}: {}", dto.getName(), e.getMessage(), e);
            throw new SourceException(ERROR_SOURCE_CREATION,
                    String.format("Failed to create source from DTO: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    @NonNull
    public Source updateSource(@NonNull Long id, @NonNull Source source) {
        log.info("Updating source with ID: {}", id);
        
        try {
            validateId(id);
            validateSource(source);
            
            Source existingSource = findSourceById(id);
            sourceMapper.updateSourceFromSource(existingSource, source);
            
            return sourceRepository.save(existingSource);
        } catch (SourceNotFoundException | SourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating source with ID {}: {}", id, e.getMessage(), e);
            throw new SourceException(ERROR_SOURCE_UPDATE,
                    String.format("Failed to update source: %s", e.getMessage()), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    public void deleteSource(@NonNull Long id) {
        log.info("Deleting source with ID: {}", id);
        
        try {
            validateId(id);
            Source source = findSourceById(id);
            sourceRepository.delete(source);
        } catch (SourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting source with ID {}: {}", id, e.getMessage(), e);
            throw new SourceException(ERROR_SOURCE_DELETION,
                    String.format("Failed to delete source: %s", e.getMessage()), e);
        }
    }

    @Override
    @Cacheable(value = "sourceNames", key = "'sourceNames'")
    @NonNull
    public Map<Long, String> getSourceNames() {
        return sourceRepository.findAll().stream()
                .collect(Collectors.toMap(Source::getId, Source::getName));
    }

    @Override
    @Cacheable(value = "sourceSearch", key = "#query")
    @NonNull
    public List<Source> findByNameContaining(@NonNull String query) {
        validateQuery(query);
        return sourceRepository.findByNameContainingIgnoreCase(query);
    }
    
    private void validateId(@NonNull Long id) {
        if (id <= 0) {
            throw new SourceException(ERROR_INVALID_ID, "ID must be positive");
        }
    }
    
    private void validateSource(@NonNull Source source) {
        if (source.getName().trim().isEmpty()) {
            throw new SourceException(ERROR_INVALID_NAME, "Source name cannot be null or empty");
        }
    }
    
    private void validateQuery(@NonNull String query) {
        if (query.trim().isEmpty()) {
            throw new SourceException(ERROR_INVALID_QUERY, "Search query cannot be empty");
        }
    }
    
    private Source findSourceById(@NonNull Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(
                        String.format("Source not found with id: %d", id)));
    }
}
