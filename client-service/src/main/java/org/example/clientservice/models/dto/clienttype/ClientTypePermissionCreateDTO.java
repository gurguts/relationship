package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientTypePermissionCreateDTO {
    @NotNull
    private Long userId;
    
    private Boolean canView = true;
    
    private Boolean canCreate = false;
    
    private Boolean canEdit = false;
    
    private Boolean canDelete = false;
}

