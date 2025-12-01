package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.UserClient;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PhoneNumber;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.IClientCrudService;
import org.example.clientservice.services.impl.ISourceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCrudService implements IClientCrudService {
    private final ClientRepository clientRepository;
    private final ISourceService sourceService;
    private final UserClient userCLient;

    @Override
    @Transactional
    public Client createClient(Client client) {
        if (client.getPhoneNumbers() != null) {
            for (PhoneNumber phoneNumber : client.getPhoneNumbers()) {
                phoneNumber.setClient(client);
            }
        }
        log.info("Creating client: {}", client.getCompany());

        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(Client client, Long id) {
        Client existingClient = getClient(id);

        String fullName = getFullName();

        String sourceName = null;
        if (existingClient.getSource() != null) {
            sourceName = sourceService.getSource(existingClient.getSource()).getName();
        }

        updateExistingClient(existingClient, client, fullName, sourceName);

        log.info("Updating client with ID: {}", id);

        return clientRepository.save(existingClient);
    }

    private String getFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();

        return userCLient.getUserFullNameFromLogin(login);
    }

    @Override
    public Client getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
    }

    @Override
    @Transactional
    public void fullDeleteClient(Long clientId) {
        getClient(clientId);
        clientRepository.deleteById(clientId);
    }

    @Override
    @Transactional
    public void deleteClient(Long clientId) {
        getClient(clientId);
        clientRepository.deactivateClientById(clientId);
    }

    @Override
    @Transactional
    public void markUrgentClient(Long clientId) {
        clientRepository.toggleUrgently(clientId);
    }

    @Override
    @Transactional
    public void setUrgentlyFalseAndRouteClient(Long clientId) {
        clientRepository.setFalseUrgentlyAndUpdateRoute(clientId, 66L);
    }

    private void updateExistingClient(Client existingClient, Client updatedClient, String fullName, String sourceName) {
        existingClient.setComment(updatedClient.getComment());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = authentication != null && authentication.getDetails() instanceof Long ? 
                (Long) authentication.getDetails() : null;
        
        boolean canEditStrangers = authentication.getAuthorities().stream()
                .anyMatch(auth -> "client_stranger:edit".equals(auth.getAuthority()));

        // Логика проверки прав доступа:
        // 1. Если у клиента нет source - можно редактировать
        // 2. Если у клиента есть source:
        //    - Если source закреплен за текущим пользователем - можно редактировать
        //    - Если source не закреплен или закреплен за другим - можно редактировать ТОЛЬКО с правом client_stranger:edit
        boolean canEditData;
        boolean isOwnClient = false; // Является ли клиент "своим" (source закреплен за пользователем или нет source)
        
        if (existingClient.getSource() == null) {
            // У клиента нет source - можно редактировать
            canEditData = true;
            isOwnClient = true;
        } else {
            // У клиента есть source
            Source clientSource = sourceService.getSource(existingClient.getSource());
            if (clientSource.getUserId() != null && currentUserId != null && 
                currentUserId.equals(clientSource.getUserId())) {
                // Source закреплен за текущим пользователем - можно редактировать
                canEditData = true;
                isOwnClient = true;
            } else {
                // Source не закреплен или закреплен за другим - можно редактировать только с правом client_stranger:edit
                canEditData = canEditStrangers;
                isOwnClient = false;
            }
        }

        // Если нет прав на редактирование - выбрасываем исключение
        if (!canEditData) {
            throw new ClientException("You do not have permission to edit this client");
        }

        // Название клиента (company) можно редактировать только для "своих" клиентов или с правом client_stranger:edit
        if (updatedClient.getCompany() != null && !isOwnClient && !canEditStrangers) {
            throw new ClientException("You do not have permission to edit client name");
        }
        
        // Обновляем название клиента только если есть права
        if (isOwnClient || canEditStrangers) {
            existingClient.setCompany(updatedClient.getCompany());
        }
        
        // Обновляем остальные поля (кроме company)
        existingClient.setPerson(updatedClient.getPerson());
        existingClient.setPricePurchase(updatedClient.getPricePurchase());
        existingClient.setPriceSale(updatedClient.getPriceSale());
        existingClient.setLocation(updatedClient.getLocation());
        existingClient.setVolumeMonth(updatedClient.getVolumeMonth());
        existingClient.setEdrpou(updatedClient.getEdrpou());
        existingClient.setEnterpriseName(updatedClient.getEnterpriseName());
        existingClient.setVat(updatedClient.getVat());

        if (updatedClient.getBusiness() != null) {
            existingClient.setBusiness(updatedClient.getBusiness());
        }

        if (updatedClient.getStatus() != null) {
            existingClient.setStatus(updatedClient.getStatus());
        }
        if (updatedClient.getRegion() != null) {
            existingClient.setRegion(updatedClient.getRegion());
        }
        if (updatedClient.getRoute() != null) {
            existingClient.setRoute(updatedClient.getRoute());
        }
        if (updatedClient.getClientProduct() != null) {
            existingClient.setClientProduct(updatedClient.getClientProduct());
        }

        if (updatedClient.getPhoneNumbers() != null) {
            existingClient.getPhoneNumbers().clear();

            updatedClient.getPhoneNumbers().forEach(phoneNumber -> {
                phoneNumber.setClient(existingClient);
                existingClient.getPhoneNumbers().add(phoneNumber);
            });
        }

        if (updatedClient.getSource() != null && !Objects.equals(updatedClient.getSource(),
                existingClient.getSource())) {
            // Изменять source могут только пользователи с правом client_stranger:edit
            if (!canEditStrangers) {
                throw new ClientException("Only users with client_stranger:edit can update source");
            }
            existingClient.setSource(updatedClient.getSource());
        }

        if (updatedClient.getFieldValues() != null) {
            existingClient.getFieldValues().clear();
            updatedClient.getFieldValues().forEach(fieldValue -> {
                fieldValue.setClient(existingClient);
                existingClient.getFieldValues().add(fieldValue);
            });
        }
    }
}


