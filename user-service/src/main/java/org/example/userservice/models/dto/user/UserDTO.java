package org.example.userservice.models.dto.user;

import lombok.Data;
import org.example.userservice.models.user.Role;
import org.example.userservice.models.user.Status;

@Data
public class UserDTO {
    private Long id;

    private String login;

    private String password;

    private String fullName;

    private Role role;

    private Status status = Status.ACTIVE;

    public UserDTO() {
    }
}
