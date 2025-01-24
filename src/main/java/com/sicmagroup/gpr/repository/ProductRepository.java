package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsDeleted(boolean deleted);

    Optional<Product> findByIdAndIsDeleted(Long id, boolean deleted);
    Optional<Product> findByUuid(String uuid);
    
}
