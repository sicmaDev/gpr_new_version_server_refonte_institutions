package com.sicmagroup.gpr.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Documentation;

public interface DocumentationRepository extends JpaRepository<Documentation, Long>{
    Optional<Documentation> findByPath(String path);
    List<Documentation> findByNameAndSize(String name, Long size);
}
