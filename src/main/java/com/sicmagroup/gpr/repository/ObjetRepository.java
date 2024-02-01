package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Objet;

public interface ObjetRepository extends JpaRepository<Objet, Long> {
      List<Objet> findByIsDeleted(boolean deleted);

    Optional<Objet> findByIdAndIsDeleted(Long id, boolean deleted);
}
