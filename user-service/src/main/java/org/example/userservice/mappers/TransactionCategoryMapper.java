package org.example.userservice.mappers;

import lombok.NonNull;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.dto.transaction.TransactionCategoryCreateDTO;
import org.example.userservice.models.dto.transaction.TransactionCategoryDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryMapper {

    public TransactionCategoryDTO transactionCategoryToTransactionCategoryDTO(@NonNull TransactionCategory category) {
        return new TransactionCategoryDTO(
                category.getId(),
                category.getType(),
                category.getName(),
                category.getDescription(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public TransactionCategory transactionCategoryCreateDTOToTransactionCategory(@NonNull TransactionCategoryCreateDTO dto) {
        TransactionCategory category = new TransactionCategory();
        category.setType(dto.getType());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return category;
    }

    public void updateTransactionCategoryFromDTO(@NonNull TransactionCategory existing, @NonNull TransactionCategoryCreateDTO dto) {
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            existing.setIsActive(dto.getIsActive());
        }
    }
}

