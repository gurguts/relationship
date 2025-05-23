package org.example.sourceservice.repositories;

import org.example.sourceservice.models.Source;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SourceRepository extends CrudRepository<Source, Long> {
    List<Source> findByNameContainingIgnoreCase(String name);
}
