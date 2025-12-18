package org.example.userservice.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.exceptions.user.UserNotFoundException;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.impl.IUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#login")
    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'allUsers'")
    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'activeUsers'")
    public List<User> getActiveUsers() {
        return userRepository.findByStatus(org.example.userservice.models.user.Status.ACTIVE);
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public void updateUserPermissions(Long id, Set<Permission> permissions) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setPermissions(permissions);
        userRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public User createUser(User user) {
        log.info(String.format("Started saving by user: %s", user.getLogin()));

        Optional<User> existingUser = userRepository.findByLogin(user.getLogin());
        if (existingUser.isPresent()) {
            throw new UserException("ALREADY_EXISTS", String.format("User with login %s already exists",
                    user.getLogin()));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        // Balance creation is now handled by Accounts
        return userRepository.save(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public void deleteUser(Long userId) {
        // Balance deletion is now handled by Accounts
        userRepository.deleteById(userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users"}, allEntries = true)
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with id: %d", user.getId())));

        existingUser.setLogin(user.getLogin());
        existingUser.setFullName(user.getFullName());
        existingUser.setRole(user.getRole());
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }

        return userRepository.save(existingUser);
    }
}
