package org.example.userservice.mappers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.NonNull;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.models.dto.user.*;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.Role;
import org.example.userservice.models.user.Status;
import org.example.userservice.models.user.User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    private static final String DEFAULT_STATUS_NAME = "ACTIVE";
    private static final Role DEFAULT_ROLE = Role.MANAGER;
    private static final Status DEFAULT_STATUS = Status.ACTIVE;
    private static final String ERROR_CODE_INVALID_ROLE = "INVALID_ROLE";
    private static final String ERROR_CODE_INVALID_STATUS = "INVALID_STATUS";
    private static final String ERROR_CODE_INVALID_PERMISSION = "PERMISSION";

    public UserIdNameDTO userToUserIdNameDTO(@NonNull User user) {
        UserIdNameDTO userIdNameDto = new UserIdNameDTO();
        userIdNameDto.setId(user.getId());
        userIdNameDto.setName(user.getFullName());
        return userIdNameDto;
    }

    public UserPageDTO userToUserPageDTO(@NonNull User user) {
        UserPageDTO userIdNameDto = new UserPageDTO();
        userIdNameDto.setId(user.getId());
        userIdNameDto.setLogin(user.getLogin());
        userIdNameDto.setRole(user.getRole().name());
        userIdNameDto.setFullName(user.getFullName());
        userIdNameDto.setStatus(user.getStatus() != null ? user.getStatus().name() : DEFAULT_STATUS_NAME);
        userIdNameDto.setAuthorities(user.getPermissions().stream()
                .map(Permission::getPermission)
                .collect(Collectors.toList()));
        return userIdNameDto;
    }

    public User userCreateDTOToUser(@NonNull UserCreateDTO userCreateDTO) {
        User user = new User();
        user.setLogin(userCreateDTO.getLogin());
        user.setPassword(userCreateDTO.getPassword());
        user.setFullName(userCreateDTO.getFullName());
        user.setRole(parseRole(userCreateDTO.getRole()));
        return user;
    }

    public UserDTO userToUserDTO(@NonNull User user) {
        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setLogin(user.getLogin());
        userDto.setPassword(user.getPassword());
        userDto.setFullName(user.getFullName());
        userDto.setRole(user.getRole());
        userDto.setStatus(user.getStatus());
        return userDto;
    }

    public User userUpdateDTOToUser(@NonNull UserUpdateDTO userUpdateDTO) {
        User user = new User();
        user.setLogin(userUpdateDTO.getLogin());
        user.setFullName(userUpdateDTO.getFullName());
        user.setRole(parseRole(userUpdateDTO.getRole()));
        if (userUpdateDTO.getStatus() != null && !userUpdateDTO.getStatus().isEmpty()) {
            user.setStatus(parseStatus(userUpdateDTO.getStatus()));
        }
        return user;
    }

    private Role parseRole(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            return DEFAULT_ROLE;
        }
        try {
            return Role.valueOf(roleString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UserException(ERROR_CODE_INVALID_ROLE,
                    String.format("Invalid role value: %s", roleString));
        }
    }

    private Status parseStatus(String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            return DEFAULT_STATUS;
        }
        try {
            return Status.valueOf(statusString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UserException(ERROR_CODE_INVALID_STATUS,
                    String.format("Invalid status value: %s", statusString));
        }
    }

    public Set<Permission> permissionStringsToPermissions(@Valid @NotEmpty @NonNull List<@NonNull String> permissionStrings) {
        return permissionStrings.stream()
                .map(this::findPermissionByValue)
                .collect(Collectors.toSet());
    }

    private Permission findPermissionByValue(@NonNull String permissionValue) {
        return Arrays.stream(Permission.values())
                .filter(permission -> permission.getPermission().equals(permissionValue))
                .findFirst()
                .orElseThrow(() -> new UserException(ERROR_CODE_INVALID_PERMISSION,
                        String.format("Invalid permission: %s", permissionValue)));
    }
}
