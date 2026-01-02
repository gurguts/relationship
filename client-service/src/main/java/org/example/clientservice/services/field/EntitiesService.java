package org.example.clientservice.services.field;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.ProductClient;
import org.example.clientservice.clients.UserClient;
import org.example.clientservice.mappers.field.SourceMapper;
import org.example.clientservice.models.dto.fields.EntitiesDTO;
import org.example.clientservice.models.dto.fields.SourceDTO;
import org.example.clientservice.models.dto.product.ProductDTO;
import org.example.clientservice.models.dto.user.UserDTO;
import org.example.clientservice.services.impl.IEntitiesService;
import org.example.clientservice.services.impl.ISourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitiesService implements IEntitiesService {
    private final ISourceService sourceService;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final SourceMapper sourceMapper;

    @Override
    @NonNull
    public EntitiesDTO getAllEntities() {
        EntitiesDTO entities = new EntitiesDTO();
        
        entities.setSources(fetchSources());
        entities.setUsers(fetchUsers());
        entities.setProducts(fetchProducts());
        
        return entities;
    }

    private List<SourceDTO> fetchSources() {
        try {
            return sourceService.getAllSources().stream()
                    .map(sourceMapper::sourceToSourceDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch sources: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<UserDTO> fetchUsers() {
        try {
            ResponseEntity<List<UserDTO>> response = userClient.getAllUsers();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("Failed to fetch users: invalid response status {}", response.getStatusCode());
            return Collections.emptyList();
        } catch (FeignException e) {
            log.error("Failed to fetch users from user-service: status={}, message={}", 
                    e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error while fetching users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<ProductDTO> fetchProducts() {
        try {
            ResponseEntity<List<ProductDTO>> response = productClient.getAllProducts();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("Failed to fetch products: invalid response status {}", response.getStatusCode());
            return Collections.emptyList();
        } catch (FeignException e) {
            log.error("Failed to fetch products from purchase-service: status={}, message={}", 
                    e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error while fetching products: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}

