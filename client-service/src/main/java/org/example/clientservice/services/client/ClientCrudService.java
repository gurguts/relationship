package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.UserClient;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PhoneNumber;
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

        String sourceName = sourceService.getSource(existingClient.getSource()).getName();

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

        boolean canEditData = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> "client:edit_strangers".equals(auth.getAuthority()));

        if (fullName.equals(sourceName) || canEditData) {
            existingClient.setCompany(updatedClient.getCompany());
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

            if (updatedClient.getPhoneNumbers() != null) {
                existingClient.getPhoneNumbers().clear();

                updatedClient.getPhoneNumbers().forEach(phoneNumber -> {
                    phoneNumber.setClient(existingClient);
                    existingClient.getPhoneNumbers().add(phoneNumber);
                });
            }

            if (updatedClient.getSource() != null && !Objects.equals(updatedClient.getSource(),
                    existingClient.getSource())) {
                boolean canEditSource = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(auth -> "client:edit_source".equals(auth.getAuthority()));

                if (!canEditSource) {
                    throw new ClientException("Only users with crm:edit_source can update sourceId");
                }
                existingClient.setSource(updatedClient.getSource());
            }
        }
    }
}

