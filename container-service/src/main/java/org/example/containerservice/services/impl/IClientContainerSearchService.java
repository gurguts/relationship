package org.example.containerservice.services.impl;

import lombok.NonNull;
import org.example.containerservice.models.dto.PageResponse;
import org.example.containerservice.models.dto.container.ClientContainerResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IClientContainerSearchService {
    PageResponse<ClientContainerResponseDTO> searchClientContainer(String query,
                                                                   @NonNull Pageable pageable,
                                                                   Map<String, List<String>> filterParams);
}
