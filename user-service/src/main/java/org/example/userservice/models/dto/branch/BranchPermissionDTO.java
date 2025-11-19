package org.example.userservice.models.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchPermissionDTO {
    private Long id;
    private Long userId;
    private Long branchId;
    private Boolean canView;
    private Boolean canOperate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


