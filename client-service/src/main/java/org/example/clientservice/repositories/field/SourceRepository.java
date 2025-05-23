package org.example.clientservice.repositories.field;

import org.example.clientservice.models.field.Source;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SourceRepository extends CrudRepository<Source, Long> {

    List<Source> findByNameContainingIgnoreCase(String name);
}
