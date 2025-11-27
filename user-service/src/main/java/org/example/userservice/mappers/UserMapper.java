package org.example.userservice.mappers;

import lombok.RequiredArgsConstructor;
import org.example.userservice.models.dto.user.*;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.Role;
import org.example.userservice.models.user.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public UserIdNameDTO userToUserIdNameDTO(User user) {
        if (user == null) {
            return null;
        }

        UserIdNameDTO userIdNameDto = new UserIdNameDTO();
        userIdNameDto.setId(user.getId());
        userIdNameDto.setName(user.getFullName());

        return userIdNameDto;
    }

    public UserPageDTO userToUserPageDTO(User user) {
        if (user == null) {
            return null;
        }

        UserPageDTO userIdNameDto = new UserPageDTO();
        userIdNameDto.setId(user.getId());
        userIdNameDto.setLogin(user.getLogin());
        userIdNameDto.setRole(user.getRole().name());
        userIdNameDto.setFullName(user.getFullName());
        userIdNameDto.setStatus(user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
        userIdNameDto.setAuthorities(user.getPermissions().stream()
                .map(Permission::getPermission)
                .collect(Collectors.toList()));

        return userIdNameDto;
    }

    public User userCreateDTOToUser(UserCreateDTO userCreateDTO) {
        if (userCreateDTO == null) {
            return null;
        }

        User user = new User();
        user.setLogin(userCreateDTO.getLogin());
        user.setPassword(userCreateDTO.getPassword());
        user.setFullName(userCreateDTO.getFullName());

        String roleString = userCreateDTO.getRole();
        if (roleString != null && !roleString.isEmpty()) {
            try {
                Role role = Role.valueOf(roleString.toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                user.setRole(Role.MANAGER);
            }
        } else {
            user.setRole(Role.MANAGER);
        }
        return user;
    }

    public UserDTO userToUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setLogin(user.getLogin());
        userDto.setPassword(user.getPassword());
        userDto.setFullName(user.getFullName());
        userDto.setRole(user.getRole());
        userDto.setStatus(user.getStatus());

        return userDto;
    }

    public User userUpdateDTOToUser(UserUpdateDTO userUpdateDTO) {
        if (userUpdateDTO == null) {
            return null;
        }

        User user = new User();
        user.setLogin(userUpdateDTO.getLogin());
        user.setFullName(userUpdateDTO.getFullName());

        String roleString = userUpdateDTO.getRole();
        if (roleString != null && !roleString.isEmpty()) {
            try {
                Role role = Role.valueOf(roleString.toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                user.setRole(Role.MANAGER);
            }
        } else {
            user.setRole(Role.MANAGER);
        }
        
        String statusString = userUpdateDTO.getStatus();
        if (statusString != null && !statusString.isEmpty()) {
            try {
                org.example.userservice.models.user.Status status = org.example.userservice.models.user.Status.valueOf(statusString.toUpperCase());
                user.setStatus(status);
            } catch (IllegalArgumentException e) {
                user.setStatus(org.example.userservice.models.user.Status.ACTIVE);
            }
        }
        return user;
    }
}
