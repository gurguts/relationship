package org.example.userservice.services.user;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.exceptions.user.UserNotFoundException;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.Status;
import org.example.userservice.models.user.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.impl.IUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private static final String ERROR_CODE_USER_ALREADY_EXISTS = "ALREADY_EXISTS";
    private static final String ERROR_CODE_LOGIN_REQUIRED = "LOGIN_REQUIRED";
    private static final String ERROR_CODE_PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    private static final String ERROR_CODE_USER_ID_REQUIRED = "USER_ID_REQUIRED";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#login")
    public User getUserByLogin(@NonNull String login) {
        if (login.trim().isEmpty()) {
            throw new UserException(ERROR_CODE_LOGIN_REQUIRED, "Login cannot be empty");
        }
        
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with login: %s", login)));
    }

    @Override
    @NonNull
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'allUsers'")
    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    @Override
    @NonNull
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'activeUsers'")
    public List<User> getActiveUsers() {
        return userRepository.findByStatus(Status.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public User getUserById(@NonNull Long id) {
        return getUserByIdOrThrow(id);
    }

    private User getUserByIdOrThrow(@NonNull Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with id: %d", id)));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public void updateUserPermissions(@NonNull Long id, @NonNull Set<Permission> permissions) {
        User user = getUserByIdOrThrow(id);
        user.setPermissions(permissions);
        userRepository.save(user);
        log.info("Updated permissions for user: id={}, permissionsCount={}", id, permissions.size());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public User createUser(@NonNull User user) {
        validateUserForCreation(user);
        
        log.info("Creating user: login={}", user.getLogin());

        if (userRepository.existsByLogin(user.getLogin())) {
            throw new UserException(ERROR_CODE_USER_ALREADY_EXISTS, 
                    String.format("User with login %s already exists", user.getLogin()));
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        
        log.info("User created: id={}, login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public void deleteUser(@NonNull Long userId) {
        User user = getUserByIdOrThrow(userId);
        userRepository.deleteById(userId);
        log.info("User deleted: id={}, login={}", userId, user.getLogin());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public User updateUser(@NonNull User user) {
        if (user.getId() == null) {
            throw new UserException(ERROR_CODE_USER_ID_REQUIRED, "User ID is required for update");
        }
        
        User existingUser = getUserByIdOrThrow(user.getId());
        
        log.info("Updating user: id={}, login={}", user.getId(), user.getLogin());

        existingUser.setLogin(user.getLogin());
        existingUser.setFullName(user.getFullName());
        existingUser.setRole(user.getRole());
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }

        User saved = userRepository.save(existingUser);
        log.info("User updated: id={}, login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    private void validateUserForCreation(@NonNull User user) {
        if (user.getLogin() == null || user.getLogin().trim().isEmpty()) {
            throw new UserException(ERROR_CODE_LOGIN_REQUIRED, "User login is required");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new UserException(ERROR_CODE_PASSWORD_REQUIRED, "User password is required");
        }
    }
}
