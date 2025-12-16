package org.example.userservice.models.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "{validation.user.login.notblank}")
    @Size(max = 255, message = "{validation.user.login.size}")
    private String login;

    @NotBlank(message = "{validation.user.password.notblank}")
    @Size(max = 255, message = "{validation.user.password.size}")
    private String password;
}
