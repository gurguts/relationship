package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    @NonNull
    List<Carrier> findByCompanyNameContainingIgnoreCase(@NonNull String companyName);

    boolean existsByCompanyNameIgnoreCase(@NonNull String companyName);
}
