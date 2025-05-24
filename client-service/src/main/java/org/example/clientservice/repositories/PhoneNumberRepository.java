package org.example.clientservice.repositories;

import org.example.clientservice.models.client.PhoneNumber;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PhoneNumberRepository extends CrudRepository<PhoneNumber, Long> {
    List<PhoneNumber> findByClientIdIn(List<Long> clientIds);
}
