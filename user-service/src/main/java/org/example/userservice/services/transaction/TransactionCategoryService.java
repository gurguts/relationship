package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionCategoryNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCategoryService {
    private static final String ERROR_CODE_CATEGORY_ALREADY_EXISTS = "CATEGORY_ALREADY_EXISTS";
    private static final String ERROR_CODE_NAME_REQUIRED = "NAME_REQUIRED";
    private static final String ERROR_CODE_TYPE_REQUIRED = "TYPE_REQUIRED";

    private final TransactionCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public @NonNull List<TransactionCategory> getCategoriesByType(@NonNull TransactionType type) {
        return categoryRepository.findByTypeAndIsActiveTrueOrderByNameAsc(type);
    }

    @Transactional(readOnly = true)
    public TransactionCategory getCategoryById(@NonNull Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new TransactionCategoryNotFoundException(
                        String.format("Transaction category with ID %d not found", id)));
    }

    @Transactional
    public TransactionCategory createCategory(@NonNull TransactionCategory category) {
        validateCategory(category);
        
        if (categoryRepository.existsByTypeAndName(category.getType(), category.getName())) {
            throw new TransactionException(ERROR_CODE_CATEGORY_ALREADY_EXISTS,
                    String.format("Category '%s' for type '%s' already exists", category.getName(), category.getType()));
        }
        
        TransactionCategory saved = categoryRepository.save(category);
        log.info("Created transaction category: id={}, type={}, name={}, isActive={}", 
                saved.getId(), saved.getType(), saved.getName(), saved.getIsActive());
        return saved;
    }

    @Transactional
    public TransactionCategory updateCategory(@NonNull Long id, @NonNull TransactionCategory updatedCategory) {
        validateCategory(updatedCategory);
        
        TransactionCategory category = getCategoryById(id);
        
        boolean nameChanged = !category.getName().equals(updatedCategory.getName());
        boolean typeChanged = !category.getType().equals(updatedCategory.getType());
        
        if (nameChanged || typeChanged) {
            if (categoryRepository.existsByTypeAndName(updatedCategory.getType(), updatedCategory.getName())) {
                TransactionCategory existing = categoryRepository.findByTypeAndName(updatedCategory.getType(), updatedCategory.getName())
                        .orElse(null);
                if (existing != null && !existing.getId().equals(id)) {
                    throw new TransactionException(ERROR_CODE_CATEGORY_ALREADY_EXISTS,
                            String.format("Category '%s' for type '%s' already exists", updatedCategory.getName(), updatedCategory.getType()));
                }
            }
        }
        
        category.setName(updatedCategory.getName());
        category.setDescription(updatedCategory.getDescription());
        category.setIsActive(updatedCategory.getIsActive());
        category.setType(updatedCategory.getType());
        
        TransactionCategory saved = categoryRepository.save(category);
        log.info("Updated transaction category: id={}, type={}, name={}, isActive={}", 
                saved.getId(), saved.getType(), saved.getName(), saved.getIsActive());
        return saved;
    }

    @Transactional
    public void deleteCategory(@NonNull Long id) {
        TransactionCategory category = getCategoryById(id);
        categoryRepository.delete(category);
        log.info("Deleted transaction category: id={}, type={}, name={}", 
                category.getId(), category.getType(), category.getName());
    }

    @Transactional
    public void deactivateCategory(@NonNull Long id) {
        TransactionCategory category = getCategoryById(id);
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Deactivated transaction category: id={}, type={}, name={}", 
                category.getId(), category.getType(), category.getName());
    }

    private void validateCategory(@NonNull TransactionCategory category) {
        if (category.getType() == null) {
            throw new TransactionException(ERROR_CODE_TYPE_REQUIRED, "Transaction category type is required");
        }
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new TransactionException(ERROR_CODE_NAME_REQUIRED, "Transaction category name is required");
        }
    }
}
