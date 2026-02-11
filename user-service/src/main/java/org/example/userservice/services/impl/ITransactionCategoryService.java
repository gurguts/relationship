package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ITransactionCategoryService {
    @NonNull List<TransactionCategory> getCategoriesByType(@NonNull TransactionType type);
    
    TransactionCategory getCategoryById(@NonNull Long id);
    
    TransactionCategory createCategory(@NonNull TransactionCategory category);
    
    TransactionCategory updateCategory(@NonNull Long id, @NonNull TransactionCategory updatedCategory);
    
    void deleteCategory(@NonNull Long id);
    
    void deactivateCategory(@NonNull Long id);

    Map<Long, String> findCategoryNamesByIds(Collection<Long> ids);
}
