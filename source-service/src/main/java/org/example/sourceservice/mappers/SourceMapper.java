package org.example.sourceservice.mappers;

import org.example.sourceservice.models.Source;
import org.example.sourceservice.models.dto.SourceCreateDTO;
import org.example.sourceservice.models.dto.SourceDTO;
import org.example.sourceservice.models.dto.SourceUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class SourceMapper {
    public Source sourceDTOtoSource(SourceDTO sourceDTO) {
        Source source = new Source();
        source.setName(sourceDTO.getName());
        return source;
    }

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
