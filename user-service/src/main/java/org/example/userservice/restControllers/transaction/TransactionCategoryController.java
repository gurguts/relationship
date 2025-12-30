package org.example.userservice.restControllers.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.models.dto.transaction.TransactionCategoryCreateDTO;
import org.example.userservice.models.dto.transaction.TransactionCategoryDTO;
import org.example.userservice.services.transaction.TransactionCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/transaction-categories")
@RequiredArgsConstructor
public class TransactionCategoryController {
    private final TransactionCategoryService categoryService;

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<TransactionCategoryDTO>> getCategoriesByType(@PathVariable TransactionType type) {
        List<TransactionCategory> categories = categoryService.getCategoriesByType(type);
        List<TransactionCategoryDTO> dtos = categories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<TransactionCategoryDTO> getCategoryById(@PathVariable Long id) {
        TransactionCategory category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(mapToDTO(category));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<TransactionCategoryDTO> createCategory(@RequestBody TransactionCategoryCreateDTO dto) {
        TransactionCategory category = new TransactionCategory();
        category.setType(dto.getType());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIsActive(true);
        TransactionCategory created = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<TransactionCategoryDTO> updateCategory(@PathVariable Long id, @RequestBody TransactionCategoryCreateDTO dto) {
        TransactionCategory category = new TransactionCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        TransactionCategory existing = categoryService.getCategoryById(id);
        category.setType(existing.getType());

        category.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : existing.getIsActive());
        TransactionCategory updated = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('settings:view')")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }

    private TransactionCategoryDTO mapToDTO(TransactionCategory category) {
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
}

