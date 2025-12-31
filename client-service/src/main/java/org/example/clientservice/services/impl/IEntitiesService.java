package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.dto.fields.EntitiesDTO;

public interface IEntitiesService {
    @NonNull
    EntitiesDTO getAllEntities();
}

