package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.dto.fields.SourceCreateDTO;
import org.example.clientservice.models.field.Source;

import java.util.List;
import java.util.Map;

public interface ISourceService {
    @NonNull
    Source getSource(@NonNull Long id);

    @NonNull
    Source updateSource(@NonNull Long id, @NonNull Source source);

    void deleteSource(@NonNull Long id);

    @NonNull
    List<Source> getAllSources();

    @NonNull
    Source createSource(@NonNull Source source);
    
    @NonNull
    Source createSource(@NonNull SourceCreateDTO dto);

    @NonNull
    Map<Long, String> getSourceNames();

    @NonNull
    List<Source> findByNameContaining(@NonNull String query);
}
