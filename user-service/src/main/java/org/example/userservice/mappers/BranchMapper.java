package org.example.userservice.mappers;

import lombok.NonNull;
import org.example.userservice.models.branch.Branch;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.models.dto.branch.BranchCreateDTO;
import org.example.userservice.models.dto.branch.BranchDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BranchMapper {

    public BranchDTO branchToBranchDTO(@NonNull Branch branch, @NonNull Map<Long, BranchPermission> permissionMap) {
        BranchPermission permission = permissionMap.get(branch.getId());
        Boolean canView = permission != null && permission.getCanView();
        Boolean canOperate = permission != null && permission.getCanOperate();

        return new BranchDTO(
                branch.getId(),
                branch.getName(),
                branch.getDescription(),
                branch.getCreatedAt(),
                branch.getUpdatedAt(),
                canView,
                canOperate
        );
    }

    public Branch branchCreateDTOToBranch(@NonNull BranchCreateDTO dto) {
        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setDescription(dto.getDescription());
        return branch;
    }
}
