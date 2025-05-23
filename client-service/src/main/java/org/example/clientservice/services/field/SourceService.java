package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.SourceNotFoundException;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.field.SourceRepository;
import org.example.clientservice.services.impl.ISourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourceService implements ISourceService {
    private final SourceRepository sourceRepository;

    @Override
    public Source getSource(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(String.format("Source not found with id: %d", id)));
    }

    @Override
    public List<Source> getAllSources() {
        return (List<Source>) sourceRepository.findAll();
    }

    @Override
    public Source createSource(Source source) {
        return sourceRepository.save(source);
    }

    @Override
    public Map<Long, String> getSourceNames() {
        List<Source> sources = (List<Source>) sourceRepository.findAll();
        return sources.stream()
                .collect(Collectors.toMap(Source::getId, Source::getName));
    }

    @Override
    @Transactional
    public Source updateSource(Long id, Source source) {
        Source existingSource = getSource(id);
        existingSource.setName(source.getName());
        return sourceRepository.save(existingSource);
    }

    @Override
    public void deleteSource(Long id) {
        Source source = getSource(id);
        sourceRepository.delete(source);
    }

    @Override
    public List<Source> findByNameContaining(String query) {
        return sourceRepository.findByNameContainingIgnoreCase(query);
    }
}
