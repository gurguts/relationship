package org.example.clientservice.repositories.field;

import lombok.NonNull;
import org.example.clientservice.models.field.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<Source, Long> {

    @NonNull
    List<Source> findByNameContainingIgnoreCase(@NonNull String name);
}
