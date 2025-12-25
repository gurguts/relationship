package org.example.userservice.models.dto.transaction;

import org.example.userservice.models.transaction.CounterpartyType;

import java.time.LocalDateTime;

public record CounterpartyDTO(
        Long id,
        CounterpartyType type,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

