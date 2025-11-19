package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.transaction.TransactionCategoryNotFoundException;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionCategoryService {
    private final TransactionCategoryRepository categoryRepository;

    public List<TransactionCategory> getCategoriesByType(TransactionType type) {
        return categoryRepository.findByTypeAndIsActiveTrueOrderByNameAsc(type);
    }

    public List<TransactionCategory> getAllCategoriesByType(TransactionType type) {
        return categoryRepository.findByTypeOrderByNameAsc(type);
    }

    public TransactionCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new TransactionCategoryNotFoundException(
                        String.format("Transaction category with ID %d not found", id)));
    }

    @Transactional
    public TransactionCategory createCategory(TransactionCategory category) {
        if (categoryRepository.existsByTypeAndName(category.getType(), category.getName())) {
            throw new IllegalArgumentException(
                    String.format("Category '%s' for type '%s' already exists", category.getName(), category.getType()));
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public TransactionCategory updateCategory(Long id, TransactionCategory updatedCategory) {
        TransactionCategory category = getCategoryById(id);
        category.setName(updatedCategory.getName());
        category.setDescription(updatedCategory.getDescription());
        category.setIsActive(updatedCategory.getIsActive());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        TransactionCategory category = getCategoryById(id);
        categoryRepository.delete(category);
    }

    @Transactional
    public void deactivateCategory(Long id) {
        TransactionCategory category = getCategoryById(id);
        category.setIsActive(false);
        categoryRepository.save(category);
    }
}

