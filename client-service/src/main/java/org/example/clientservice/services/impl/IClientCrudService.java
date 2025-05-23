package org.example.clientservice.services.impl;

import org.example.clientservice.models.client.Client;

public interface IClientCrudService {
    Client createClient(Client client);

    Client updateClient(Client client, Long id);

    Client getClient(Long clientId);

    void fullDeleteClient(Long clientId);

    void deleteClient(Long clientId);

    void markUrgentClient(Long clientId);

    void setUrgentlyFalseAndRouteClient(Long clientId);
}