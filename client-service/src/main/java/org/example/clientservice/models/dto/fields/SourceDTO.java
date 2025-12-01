package org.example.clientservice.models.dto.fields;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourceDTO {
    private Long id;
    private String name;
    private Long userId;
}