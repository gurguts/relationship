package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

@Data
public class ClientTypePermissionUpdateDTO {
    private Boolean canView;
    
    private Boolean canCreate;
    
    private Boolean canEdit;
    
    private Boolean canDelete;
}

