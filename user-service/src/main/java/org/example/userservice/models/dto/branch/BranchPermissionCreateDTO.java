package org.example.userservice.models.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchPermissionCreateDTO {
    private Long userId;
    private Long branchId;
    private Boolean canView;
    private Boolean canOperate;
}


