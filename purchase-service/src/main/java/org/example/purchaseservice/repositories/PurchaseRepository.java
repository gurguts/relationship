package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.Purchase;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {
    @NotNull
    Page<Purchase> findAll(Specification<Purchase> spec, @NotNull Pageable pageable);

    List<Purchase> findByClient(Long clientId);
}
