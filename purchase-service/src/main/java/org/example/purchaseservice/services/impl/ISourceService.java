package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.dto.fields.SourceDTO;

import java.util.List;

public interface ISourceService {
    SourceDTO getSourceName(@NonNull Long sourceId);
    
    List<SourceDTO> findByNameContaining(@NonNull String query);
}
