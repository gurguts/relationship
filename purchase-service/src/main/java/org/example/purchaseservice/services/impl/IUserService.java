package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.dto.user.UserDTO;

import java.util.List;

public interface IUserService {
    String getUserFullNameFromLogin(@NonNull String login);
    
    List<UserDTO> getAllUsers();
}
