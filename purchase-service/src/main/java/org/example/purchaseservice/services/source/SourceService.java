package org.example.purchaseservice.services.source;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.SourceClient;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {
    
    private final SourceClient sourceClient;
    
    public SourceDTO getSourceName(@NonNull Long sourceId) {
        try {
            SourceDTO sourceDTO = sourceClient.getSourceName(sourceId).getBody();
            if (sourceDTO == null) {
                throw new PurchaseException("SOURCE_NOT_FOUND", 
                        String.format("Source with ID %d not found", sourceId));
            }
            return sourceDTO;
        } catch (PurchaseException e) {
            throw e;
        } catch (FeignException e) {
            log.error("Feign error getting source name: sourceId={}, status={}, error={}", 
                    sourceId, e.status(), e.getMessage(), e);
            throw new PurchaseException("SOURCE_FETCH_ERROR", 
                    String.format("Failed to fetch source: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error getting source name: sourceId={}, error={}", 
                    sourceId, e.getMessage(), e);
            throw new PurchaseException("SOURCE_FETCH_ERROR", 
                    String.format("Failed to fetch source: %s", e.getMessage()), e);
        }
    }
    
    public List<SourceDTO> findByNameContaining(@NonNull String query) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<SourceDTO> sources = sourceClient.findByNameContaining(query).getBody();
            return sources != null ? sources : Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error finding sources by name: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error finding sources by name: query={}, error={}", 
                    query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}

