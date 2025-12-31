package org.example.clientservice.mappers.field;

import lombok.NonNull;
import org.example.clientservice.models.dto.fields.SourceCreateDTO;
import org.example.clientservice.models.dto.fields.SourceDTO;
import org.example.clientservice.models.dto.fields.SourceUpdateDTO;
import org.example.clientservice.models.field.Source;
import org.springframework.stereotype.Component;

@Component
public class SourceMapper {

    public SourceDTO sourceToSourceDTO(@NonNull Source source) {
        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setName(source.getName());
        sourceDTO.setId(source.getId());
        sourceDTO.setUserId(source.getUserId());
        return sourceDTO;
    }

    public Source sourceCreateDTOtoSource(@NonNull SourceCreateDTO dto) {
        Source source = new Source();
        source.setName(dto.getName());
        source.setUserId(dto.getUserId());
        return source;
    }

    public Source sourceUpdateDTOtoSource(@NonNull SourceUpdateDTO dto) {
        Source source = new Source();
        source.setName(dto.getName());
        source.setUserId(dto.getUserId());
        return source;
    }

    public void updateSourceFromSource(@NonNull Source existingSource, @NonNull Source source) {
        existingSource.setName(source.getName());
        if (source.getUserId() != null) {
            existingSource.setUserId(source.getUserId());
        }
    }
}
