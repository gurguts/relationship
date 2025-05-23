package org.example.userservice.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.exceptions.user.UserNotFoundException;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.models.balance.Balance;
import org.example.userservice.models.dto.user.UserBalanceDTO;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.impl.IBalanceService;
import org.example.userservice.services.impl.IUserService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IBalanceService balanceService;
    private final UserMapper userMapper;

    @Override
    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<UserBalanceDTO> getUserBalances() {
        log.info("Fetching user balances");

        List<User> users = userRepository.findAll();

        List<Long> userIds = users.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Balance> balances = balanceService.findByUserIdIn(userIds);

        Map<Long, Balance> balanceMap = balances.stream()
                .collect(Collectors.toMap(Balance::getUserId, balance -> balance));

        return users.stream()
                .map(user -> {
                    Map<String, BigDecimal> balancesResult = new HashMap<>();
                    Balance balance = balanceMap.get(user.getId());
                    if (balance != null) {
                        balancesResult.put("UAH", balance.getBalanceUAH());
                        balancesResult.put("EUR", balance.getBalanceEUR());
                        balancesResult.put("USD", balance.getBalanceUSD());
                    } else {
                        balancesResult.put("UAH", BigDecimal.ZERO);
                        balancesResult.put("EUR", BigDecimal.ZERO);
                        balancesResult.put("USD", BigDecimal.ZERO);
                    }
                    return userMapper.userToUserBalanceDTO(user, balancesResult);
                })
                .toList();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void updateUserPermissions(Long id, Set<Permission> permissions) {
        User user = getUserById(id);
        user.setPermissions(permissions);
        userRepository.save(user);
    }

    @Override
    public User createUser(User user) {
        log.info(String.format("Started saving by user: %s", user.getLogin()));

        Optional<User> existingUser = userRepository.findByLogin(user.getLogin());
        if (existingUser.isPresent()) {
            throw new UserException("ALREADY_EXISTS", String.format("User with login %s already exists",
                    user.getLogin()));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        balanceService.createUserBalance(user.getId());
        return userRepository.save(savedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        balanceService.deleteBalanceUser(userId);
        userRepository.deleteById(userId);
    }

    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with id: %d", user.getId())));

        existingUser.setLogin(user.getLogin());
        existingUser.setFullName(user.getFullName());
        existingUser.setRole(user.getRole());

        return userRepository.save(existingUser);
    }
}
