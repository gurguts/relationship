package org.example.userservice.restControllers.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.mappers.UserMapper;
import org.example.userservice.models.dto.user.*;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;
import org.example.userservice.services.impl.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{login}")
    public ResponseEntity<String> getFullNameByLogin(@PathVariable String login) {
        return ResponseEntity.ok(userService.getUserByLogin(login).getFullName());
    }

    @GetMapping
    public ResponseEntity<List<UserIdNameDTO>> getUsers() {
        List<User> users = userService.getUsers();
        List<UserIdNameDTO> userIdNameDTO = users.stream()
                .map(userMapper::userToUserIdNameDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userIdNameDTO);
    }

    @GetMapping("/page")
    public ResponseEntity<List<UserPageDTO>> getUsersForPage() {
        List<User> users = userService.getUsers();
        List<UserPageDTO> userPageDTO = users.stream()
                .map(userMapper::userToUserPageDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userPageDTO);
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<String>> getUserPermissions(@PathVariable Long id) {
        User user = userService.getUserById(id);
        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getPermission)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    @PreAuthorize("hasAuthority('user:edit')")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<Void> updateUserPermissions(@PathVariable Long id,
                                                      @RequestBody List<String> permissionStrings) {
        Set<Permission> permissions = permissionStrings.stream()
                .map(this::findPermissionByValue)
                .collect(Collectors.toSet());
        userService.updateUserPermissions(id, permissions);
        return ResponseEntity.ok().build();
    }

    private Permission findPermissionByValue(String permissionValue) {
        return Arrays.stream(Permission.values())
                .filter(permission -> permission.getPermission().equals(permissionValue))
                .findFirst()
                .orElseThrow(() -> new UserException("PERMISSION",
                        String.format("Invalid permission: %s", permissionValue)));
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid UserCreateDTO userCreateDTO) {
        User user = userMapper.userCreateDTOToUser(userCreateDTO);
        UserDTO createdUser = userMapper.userToUserDTO(userService.createUser(user));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdUser);
    }

    @PreAuthorize("hasAuthority('user:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO userUpdateDTO) {
        User user = userMapper.userUpdateDTOToUser(userUpdateDTO);
        user.setId(id);
        UserDTO updatedUser = userMapper.userToUserDTO(userService.updateUser(user));
        return ResponseEntity.ok(updatedUser);
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
