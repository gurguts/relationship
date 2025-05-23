package org.example.saleservice.repositories;

import org.example.saleservice.models.Sale;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {
    @NotNull
    Page<Sale> findAll(Specification<Sale> spec, @NotNull Pageable pageable);

    List<Sale> findByClient(Long clientId);
}
