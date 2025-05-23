package org.example.clientservice.services.impl;


import org.example.clientservice.models.field.Source;

import java.util.List;
import java.util.Map;

public interface ISourceService {
    Source getSource(Long id);

    Source updateSource(Long id, Source source);

    void deleteSource(Long id);

    List<Source> getAllSources();

    Source createSource(Source source);

    Map<Long, String> getSourceNames();

    List<Source> findByNameContaining(String query);
}
