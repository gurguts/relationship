package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductTransferRepository extends JpaRepository<ProductTransfer, Long>, JpaSpecificationExecutor<ProductTransfer> {
    
    Page<ProductTransfer> findAll(Specification<ProductTransfer> specification, Pageable pageable);
    
    List<ProductTransfer> findAll(Specification<ProductTransfer> specification);
}

