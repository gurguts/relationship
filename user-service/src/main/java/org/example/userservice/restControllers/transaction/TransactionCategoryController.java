package org.example.userservice.restControllers.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.mappers.TransactionCategoryMapper;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.models.dto.transaction.TransactionCategoryCreateDTO;
import org.example.userservice.models.dto.transaction.TransactionCategoryDTO;
import org.example.userservice.services.impl.ITransactionCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/transaction-categories")
@RequiredArgsConstructor
@Validated
public class TransactionCategoryController {
    private final ITransactionCategoryService categoryService;
    private final TransactionCategoryMapper transactionCategoryMapper;

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<TransactionCategoryDTO>> getCategoriesByType(
            @PathVariable @NonNull TransactionType type) {
        List<TransactionCategory> categories = categoryService.getCategoriesByType(type);
        List<TransactionCategoryDTO> dtos = categories.stream()
                .map(transactionCategoryMapper::transactionCategoryToTransactionCategoryDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/batch")
    public ResponseEntity<Map<Long, String>> getNamesByIds(
            @RequestParam("ids") Set<Long> ids) {

        Map<Long, String> names = categoryService.findCategoryNamesByIds(ids);
        return ResponseEntity.ok(names);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<TransactionCategoryDTO> getCategoryById(
            @PathVariable @Positive @NonNull Long id) {
        TransactionCategory category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(transactionCategoryMapper.transactionCategoryToTransactionCategoryDTO(category));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<TransactionCategoryDTO> createCategory(
            @RequestBody @Valid @NonNull TransactionCategoryCreateDTO dto) {
        TransactionCategory category = transactionCategoryMapper.transactionCategoryCreateDTOToTransactionCategory(dto);
        TransactionCategory created = categoryService.createCategory(category);
        TransactionCategoryDTO categoryDTO = transactionCategoryMapper.transactionCategoryToTransactionCategoryDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(categoryDTO.getId())
                .toUri();
        return ResponseEntity.status(CREATED).location(location).body(categoryDTO);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<TransactionCategoryDTO> updateCategory(
            @PathVariable @Positive @NonNull Long id,
            @RequestBody @Valid @NonNull TransactionCategoryCreateDTO dto) {
        TransactionCategory existing = categoryService.getCategoryById(id);
        transactionCategoryMapper.updateTransactionCategoryFromDTO(existing, dto);
        TransactionCategory updated = categoryService.updateCategory(id, existing);
        return ResponseEntity.ok(transactionCategoryMapper.transactionCategoryToTransactionCategoryDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteCategory(@PathVariable @Positive @NonNull Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('settings:view')")
    public ResponseEntity<Void> deactivateCategory(@PathVariable @Positive @NonNull Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }
}
