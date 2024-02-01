package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Poste;

// you need to import the entity class arccording to your context 
public interface PosteRepository extends JpaRepository<Poste, Long> {

    List<Poste> findByIsDeleted(boolean deleted);

    Optional<Poste> findByIdAndIsDeleted(Long id, boolean deleted);
}