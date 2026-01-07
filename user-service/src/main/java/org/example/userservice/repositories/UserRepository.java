package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    @EntityGraph(attributePaths = {"permissions"})
    Optional<User> findByLogin(@NonNull String login);

    @NonNull
    @EntityGraph(attributePaths = {"permissions"})
    List<User> findAll();

    @NonNull
    @EntityGraph(attributePaths = {"permissions"})
    Optional<User> findById(@NonNull Long id);
    
    @NonNull
    @EntityGraph(attributePaths = {"permissions"})
    List<User> findByStatus(@NonNull org.example.userservice.models.user.Status status);
    
    boolean existsByLogin(@NonNull String login);
}
