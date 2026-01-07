package org.example.userservice.mappers;

import lombok.NonNull;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.models.dto.branch.BranchPermissionDTO;
import org.springframework.stereotype.Component;

@Component
public class BranchPermissionMapper {

    public BranchPermissionDTO branchPermissionToBranchPermissionDTO(@NonNull BranchPermission permission) {
        return new BranchPermissionDTO(
                permission.getId(),
                permission.getUserId(),
                permission.getBranchId(),
                permission.getCanView(),
                permission.getCanOperate(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}

