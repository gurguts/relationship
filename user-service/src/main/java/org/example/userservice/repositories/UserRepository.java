package org.example.userservice.repositories;

import org.example.userservice.models.user.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    @EntityGraph(attributePaths = {"permissions"})
    Optional<User> findByLogin(String login);

    @NotNull
    @EntityGraph(attributePaths = {"permissions"})
    List<User> findAll();

    @NotNull
    @EntityGraph(attributePaths = {"permissions"})
    Optional<User> findById(@NotNull Long id);
}
