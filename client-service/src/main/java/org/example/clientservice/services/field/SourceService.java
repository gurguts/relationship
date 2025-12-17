package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.SourceNotFoundException;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.field.SourceRepository;
import org.example.clientservice.services.impl.ISourceService;
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
    private final SourceRepository sourceRepository;

    @Override
    @Cacheable(value = "sources", key = "#id")
    public Source getSource(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException(String.format("Source not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "sources", key = "'allSources'")
    public List<Source> getAllSources() {
        return (List<Source>) sourceRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    public Source createSource(Source source) {
        return sourceRepository.save(source);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    public Source updateSource(Long id, Source source) {
        Source existingSource = findSource(id);
        existingSource.setName(source.getName());
        if (source.getUserId() != null) {
            existingSource.setUserId(source.getUserId());
        }
        return sourceRepository.save(existingSource);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"sources", "sourceNames", "sourceSearch"}, allEntries = true)
    public void deleteSource(Long id) {
        Source source = findSource(id);
        sourceRepository.delete(source);
    }

    @Override
    @Cacheable(value = "sourceNames", key = "'sourceNames'")
    public Map<Long, String> getSourceNames() {
        List<Source> sources = getAllSources();
        return sources.stream()
                .collect(Collectors.toMap(Source::getId, Source::getName));
    }

    @Override
    @Cacheable(value = "sourceSearch", key = "#query")
    public List<Source> findByNameContaining(String query) {
        return sourceRepository.findByNameContainingIgnoreCase(query);
    }

    private Source findSource(Long id) {
        return sourceRepository.findById(id).orElseThrow(() ->
                new SourceNotFoundException(String.format("Source not found with id: %d", id)));
    }
}
