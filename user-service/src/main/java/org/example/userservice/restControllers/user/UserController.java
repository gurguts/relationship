package org.example.userservice.restControllers.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.models.dto.user.*;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;
import org.example.userservice.services.impl.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final IUserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{login}")
    public ResponseEntity<String> getFullNameByLogin(@PathVariable @NonNull String login) {
        User user = userService.getUserByLogin(login);
        if (user == null) {
            throw new UserException("USER_NOT_FOUND", String.format("User with login %s not found", login));
        }
        return ResponseEntity.ok(user.getFullName());
    }

    @GetMapping
    public ResponseEntity<List<UserIdNameDTO>> getUsers() {
        List<User> users = userService.getActiveUsers();
        List<UserIdNameDTO> userIdNameDTO = users.stream()
                .map(userMapper::userToUserIdNameDTO)
                .toList();
        return ResponseEntity.ok(userIdNameDTO);
    }

    @GetMapping("/page")
    public ResponseEntity<List<UserPageDTO>> getUsersForPage() {
        List<User> users = userService.getUsers();
        List<UserPageDTO> userPageDTO = users.stream()
                .map(userMapper::userToUserPageDTO)
                .toList();
        return ResponseEntity.ok(userPageDTO);
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<String>> getUserPermissions(@PathVariable @Positive @NonNull Long id) {
        User user = userService.getUserById(id);
        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getPermission)
                .toList();
        return ResponseEntity.ok(permissions);
    }

    @PreAuthorize("hasAuthority('user:edit')")
    @PatchMapping("/{id}/permissions")
    public ResponseEntity<Void> updateUserPermissions(
            @PathVariable @Positive @NonNull Long id,
            @RequestBody @Valid @NotEmpty @NonNull List<@NonNull String> permissionStrings) {
        Set<Permission> permissions = userMapper.permissionStringsToPermissions(permissionStrings);
        userService.updateUserPermissions(id, permissions);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid @NonNull UserCreateDTO userCreateDTO) {
        User user = userMapper.userCreateDTOToUser(userCreateDTO);
        UserDTO createdUser = userMapper.userToUserDTO(userService.createUser(user));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdUser);
    }

    @PreAuthorize("hasAuthority('user:edit')")
    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable @Positive @NonNull Long id,
            @RequestBody @Valid @NonNull UserUpdateDTO userUpdateDTO) {
        User user = userMapper.userUpdateDTOToUser(userUpdateDTO);
        user.setId(id);
        UserDTO updatedUser = userMapper.userToUserDTO(userService.updateUser(user));
        return ResponseEntity.ok(updatedUser);
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive @NonNull Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
