package org.example.userservice.services.impl;

import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;

import java.util.List;
import java.util.Set;

public interface IUserService {
    User getUserByLogin(String login);

    List<User> getUsers();
    
    List<User> getActiveUsers();


    User getUserById(Long id);

    void updateUserPermissions(Long id, Set<Permission> permissions);

    User createUser(User user);

    void deleteUser(Long userId);

    User updateUser(User user);
}
