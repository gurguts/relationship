package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.ClientProductNotFoundException;
import org.example.clientservice.models.field.ClientProduct;
import org.example.clientservice.repositories.field.ClientProductRepository;
import org.example.clientservice.services.impl.IClientProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientProductService implements IClientProductService {
    private final ClientProductRepository clientProductRepository;

    @Override
    @Cacheable(value = "clientProducts", key = "#id")
    public ClientProduct getClientProduct(Long id) {
        return clientProductRepository.findById(id).orElseThrow(() ->
                new ClientProductNotFoundException(String.format("ClientProduct not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "clientProducts", key = "'allClientProducts'")
    public List<ClientProduct> getAllClientProducts() {
        return (List<ClientProduct>) clientProductRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"clientProducts", "clientProductNames", "clientProductSearch"}, allEntries = true)
    public ClientProduct createClientProduct(ClientProduct clientProduct) {
        clientProduct.setId(null);
        return clientProductRepository.save(clientProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"clientProducts", "clientProductNames", "clientProductSearch"}, allEntries = true)
    public ClientProduct updateClientProduct(Long id, ClientProduct clientProduct) {
        ClientProduct oldClientProduct = findClientProduct(id);
        oldClientProduct.setName(clientProduct.getName());
        return clientProductRepository.save(oldClientProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"clientProducts", "clientProductNames", "clientProductSearch"}, allEntries = true)
    public void deleteClientProduct(Long id) {
        ClientProduct clientProduct = findClientProduct(id);
        clientProductRepository.delete(clientProduct);
    }

    @Override
    @Cacheable(value = "ClientProductNames", key = "'ClientProductNames'")
    public Map<Long, String> getClientProductNames() {
        List<ClientProduct> clientProducts = (List<ClientProduct>) clientProductRepository.findAll();
        return clientProducts.stream()
                .collect(Collectors.toMap(ClientProduct::getId, ClientProduct::getName));
    }

    @Override
    @Cacheable(value = "clientProductSearch", key = "#query")
    public List<ClientProduct> findByNameContaining(String query) {
        return clientProductRepository.findByNameContainingIgnoreCase(query);
    }

    private ClientProduct findClientProduct(Long id) {
        return clientProductRepository.findById(id).orElseThrow(() ->
                new ClientProductNotFoundException(String.format("ClientProduct not found with id: %d", id)));
    }
}
