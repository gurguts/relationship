package org.example.containerservice.repositories;

import org.example.containerservice.models.Container;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerRepository extends JpaRepository<Container, Long> {
}
