package org.example.purchaseservice.services.source;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.SourceClient;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {
    
    private final SourceClient sourceClient;
    
    @Cacheable(value = "sourceNames", key = "#sourceId")
    public SourceDTO getSourceName(@NonNull Long sourceId) {
        log.debug("Getting source name: sourceId={}", sourceId);
        try {
            SourceDTO sourceDTO = sourceClient.getSourceName(sourceId).getBody();
            if (sourceDTO == null) {
                log.warn("Source not found: sourceId={}", sourceId);
                throw new PurchaseException("SOURCE_NOT_FOUND", 
                        String.format("Source with ID %d not found", sourceId));
            }
            log.debug("Source name found: sourceId={}, name={}", sourceId, sourceDTO.getName());
            return sourceDTO;
        } catch (PurchaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get source name: sourceId={}, error={}", sourceId, e.getMessage(), e);
            throw new PurchaseException("SOURCE_FETCH_ERROR", 
                    String.format("Failed to fetch source: %s", e.getMessage()), e);
        }
    }
    
    public List<SourceDTO> findByNameContaining(@NonNull String query) {
        log.debug("Finding sources by name containing: query={}", query);
        try {
            List<SourceDTO> sources = sourceClient.findByNameContaining(query).getBody();
            if (sources == null) {
                log.warn("No sources found for query: {}", query);
                return List.of();
            }
            log.debug("Found {} sources for query: {}", sources.size(), query);
            return sources;
        } catch (Exception e) {
            log.error("Failed to find sources by name: query={}, error={}", query, e.getMessage(), e);
            return List.of();
        }
    }
    
    @CacheEvict(value = "sourceNames", key = "#sourceId")
    public void evictSourceNameCache(@NonNull Long sourceId) {
        log.debug("Evicting source name cache: sourceId={}", sourceId);
    }
    
    @CacheEvict(value = "sourceNames", allEntries = true)
    public void evictAllSourceNamesCache() {
        log.debug("Evicting all source names cache");
    }
}

