package org.example.clientservice.mappers.field;

import org.example.clientservice.models.dto.fields.SourceCreateDTO;
import org.example.clientservice.models.dto.fields.SourceDTO;
import org.example.clientservice.models.dto.fields.SourceUpdateDTO;
import org.example.clientservice.models.field.Source;
import org.springframework.stereotype.Component;

@Component
public class SourceMapper {

    public SourceDTO sourceToSourceDTO(Source source) {
        SourceDTO sourceDTO = new SourceDTO();
        sourceDTO.setName(source.getName());
        sourceDTO.setId(source.getId());
        return sourceDTO;
    }

    public Source sourceCreateDTOtoSource(SourceCreateDTO dto) {
        Source source = new Source();
        source.setName(dto.getName());
        return source;
    }

    public Source sourceUpdateDTOtoSource(SourceUpdateDTO dto) {
        Source source = new Source();
        source.setName(dto.getName());
        return source;
    }
}
