package org.example.userservice.mappers;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.models.user.Permission;

import java.util.Arrays;

@Converter(autoApply = true)
public class PermissionConverter implements AttributeConverter<Permission, String> {

    private static final String ERROR_CODE_UNKNOWN_PERMISSION = "UNKNOWN_PERMISSION";

    @Override
    public String convertToDatabaseColumn(Permission permission) {
        return permission != null ? permission.getPermission() : null;
    }

    @Override
    public Permission convertToEntityAttribute(String permissionValue) {
        if (permissionValue == null) {
            return null;
        }
        return Arrays.stream(Permission.values())
                .filter(permission -> permission.getPermission().equals(permissionValue))
                .findFirst()
                .orElseThrow(() -> new UserException(ERROR_CODE_UNKNOWN_PERMISSION,
                        String.format("Unknown permission value: %s", permissionValue)));
    }
}