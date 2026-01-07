package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;

import java.util.List;
import java.util.Set;

public interface IUserService {
    User getUserByLogin(@NonNull String login);

    @NonNull List<User> getUsers();

    @NonNull List<User> getActiveUsers();

    User getUserById(@NonNull Long id);

    void updateUserPermissions(@NonNull Long id, @NonNull Set<Permission> permissions);

    User createUser(@NonNull User user);

    void deleteUser(@NonNull Long userId);

    User updateUser(@NonNull User user);
}
